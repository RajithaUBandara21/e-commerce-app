package com.rajitha.ecommerce.service.serviceImpl;
import com.rajitha.ecommerce.client.feign.CustomerClient;
import com.rajitha.ecommerce.dto.*;
import com.rajitha.ecommerce.messaging.OrderCreatedProducer;
import com.rajitha.ecommerce.mapper.OrderMapper;
import com.rajitha.ecommerce.repository.OrderRepository;
import com.rajitha.ecommerce.service.OrderLineService;
import com.rajitha.ecommerce.service.OrderService;
import com.rajitha.ecommerce.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final CustomerClient customerClient;
    private final OrderRepository orderRepository;
    private final OrderLineService orderLineService;
    private final OrderCreatedProducer orderCreatedProducer;


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

//        persist oder as PENDING_PAYMENT -- stock reservation and payment happen asynchronously from here
        var orderToSave = orderMapper.toOder(orderRequestDTO);
        orderToSave.setIdempotencyKey(idempotencyKey);
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
                        .totalAmount(orderRequestDTO.totalAmount())
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
}
