package com.rajitha.ecommerce.service.serviceImpl;
import com.rajitha.ecommerce.client.feign.CustomerClient;
import com.rajitha.ecommerce.client.feign.PaymentClient;
import com.rajitha.ecommerce.client.rest.ProductClient;
import com.rajitha.ecommerce.dto.*;
import com.rajitha.ecommerce.messaging.OrderProducer;
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
    private final ProductClient productClient ;
    private final OrderRepository orderRepository;
    private final OrderLineService orderLineService;
    private final OrderProducer orderProducer;
    private final PaymentClient paymentClient;
    @Override
    public Integer createOrder(OrderRequestDTO orderRequestDTO) {
//        check the customer -> OpenFeign
        var customer  = customerClient.findCustomerById(orderRequestDTO.customerId()).orElseThrow(()->new BusinessException ("Cannot create order :: no customer exists with provided id: " + orderRequestDTO.customerId() ));

//        purchase the products ->product-ms (use Rest template)
        var purchaseProduct = productClient.purchaseProducts(orderRequestDTO.products());

//        persist oder
        var order = orderRepository.save(orderMapper.toOder(orderRequestDTO));

//        persist order line
        for(PurchaseRequestDTO purchaseRequestDTO : orderRequestDTO.products()){
            orderLineService.saveOrderLine(
                    new OrderLineRequestDTO(
                            null,
                            order.getId(),
                            purchaseRequestDTO.productId(),
                            purchaseRequestDTO.quantity()
                                            )
            );
        }
//        Start payment process
var paymentRequestDTO = new PaymentRequestDTO(

        orderRequestDTO.totalAmount(),
        orderRequestDTO.paymentMethode(),
        orderRequestDTO.id(),
        orderRequestDTO.reference(),
        customer

);
paymentClient.requestOrderPayment(paymentRequestDTO);
//        send the order conform  --> notification-ms(kafka)
        orderProducer.sendOrderConformation( OrderConfirmationDTO.builder()
                        .orderReference(orderRequestDTO.reference())
                        .customerResponseDTO(customer)
                        .totalAmount(orderRequestDTO.totalAmount())
                        .paymentMethode(orderRequestDTO.paymentMethode())
                        .products(purchaseProduct)

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
