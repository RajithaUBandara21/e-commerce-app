package com.rajitha.ecommerce.service.serviceImpl;
import com.rajitha.ecommerce.client.feign.CustomerClient;
import com.rajitha.ecommerce.client.feign.PaymentClient;
import com.rajitha.ecommerce.dto.*;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.exception.OrderAccessDeniedException;
import com.rajitha.ecommerce.messaging.OrderCreatedProducer;
import com.rajitha.ecommerce.mapper.OrderMapper;
import com.rajitha.ecommerce.messaging.StockReleaseProducer;
import com.rajitha.ecommerce.repository.OrderRepository;
import com.rajitha.ecommerce.service.CouponService;
import com.rajitha.ecommerce.service.OrderLineService;
import com.rajitha.ecommerce.service.OrderService;
import com.rajitha.ecommerce.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final EnumSet<OrderStatus> REFUNDABLE_STATUSES =
            EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final OrderMapper orderMapper;
    private final CustomerClient customerClient;
    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;
    private final OrderLineService orderLineService;
    private final OrderCreatedProducer orderCreatedProducer;
    private final StockReleaseProducer stockReleaseProducer;
    private final CouponService couponService;


    @Override
    public Integer createOrder(OrderRequestDTO orderRequestDTO, String idempotencyKey) {
//        replay of an already-processed request -> return the existing order, don't redo side effects
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                return existingOrder.get().getId();
            }
        }

//        check the customer -> OpenFeign (fail fast before anything is persisted)
        CustomerResponseDTO customer  = customerClient.findCustomerById(orderRequestDTO.customerId()).orElseThrow(()->new BusinessException ("Cannot create order :: no customer exists with provided id: " + orderRequestDTO.customerId() ));

//        coupon discount, if any, is applied to the same client-supplied totalAmount
//        the rest of this flow already trusts (see PLAN.md's note on totalAmount not
//        being server-recomputed from authoritative variant prices — not a new gap)
        var effectiveTotal = orderRequestDTO.totalAmount();
        if (orderRequestDTO.couponCode() != null && !orderRequestDTO.couponCode().isBlank()) {
            effectiveTotal = couponService.applyDiscount(orderRequestDTO.couponCode(), orderRequestDTO.totalAmount());
        }

//        persist oder as PENDING_PAYMENT -- stock reservation and payment happen asynchronously from here
        var orderToSave = orderMapper.toOder(orderRequestDTO);
        orderToSave.setIdempotencyKey(idempotencyKey);
        orderToSave.setTotalAmount(effectiveTotal);
        var order = orderRepository.save(orderToSave);

//        persist order line (records what was requested, regardless of eventual reservation outcome)
        for(PurchaseRequestDTO products :orderRequestDTO.products()){
          orderLineService.saveOrderLine(
                    new OrderLineRequestDTO(
                            null,
                            order.getId(),
                            products.variantId(),
                            products.quantity()
                                            )
            );
        }

//        hand off to the saga: product-service reserves stock, then payment-service charges
        orderCreatedProducer.sendOrderCreated(
                OrderCreatedEventDTO.builder()
                        .orderReference(order.getReference())
                        .totalAmount(effectiveTotal)
                        .paymentMethode(orderRequestDTO.paymentMethode())
                        .stripePaymentMethodId(orderRequestDTO.stripePaymentMethodId())
                        .customer(customer)
                        .products(orderRequestDTO.products())
                        .build()
        );

        return order.getId();
    }

    @Override
    public List<OrderResponseDTO> findAllOderResponses() {
        return orderRepository.findAll().stream().map(orderMapper::toOrderResponseDTO).collect(Collectors.toList());
    }

    @Override
    public OrderResponseDTO getOderById(Integer orderId) {
        return orderRepository.findById(orderId).map(orderMapper::toOrderResponseDTO).orElseThrow( () -> new EntityNotFoundException("Order not found :: for oder id: " + orderId));
    }

    @Override
    public List<OrderResponseDTO> findMyOrders(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(orderMapper::toOrderResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponseDTO refundOrder(Integer orderId, String callerId, boolean isAdmin) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found :: for oder id: " + orderId));

        if (!isAdmin && !callerId.equals(order.getCustomerId())) {
            throw new OrderAccessDeniedException("You do not own order " + orderId);
        }

        if (!REFUNDABLE_STATUSES.contains(order.getStatus())) {
            throw new BusinessException("Order " + orderId + " is " + order.getStatus() + " — cannot be refunded");
        }

        var refundResult = paymentClient.refund(PaymentRefundRequestDTO.builder().orderReference(order.getReference()).build());
        if (!refundResult.success()) {
            throw new BusinessException("Refund failed: " + refundResult.reason());
        }

        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        // Reuses the exact compensation path payment-failure already goes through —
        // same topic, same consumer on product-service's side, no new stock-restore
        // code path needed.
        var linesToRelease = orderLineService.findOrderLineByOrderId(order.getId()).stream()
                .map(line -> new PurchaseRequestDTO(line.variantId(), line.quantity()))
                .toList();

        stockReleaseProducer.sendStockRelease(
                StockReleaseEventDTO.builder()
                        .orderReference(order.getReference())
                        .products(linesToRelease)
                        .build()
        );

        return orderMapper.toOrderResponseDTO(order);
    }
}
