package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.feign.CustomerClient;
import com.rajitha.ecommerce.client.rest.ProductClient;
import com.rajitha.ecommerce.dto.OrderConformationDTO;
import com.rajitha.ecommerce.dto.OrderLineRequestDTO;
import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.kafka.OrderProducer;
import com.rajitha.ecommerce.mapper.OrderMapper;
import com.rajitha.ecommerce.repository.OrderRepository;
import com.rajitha.ecommerce.service.OrderLineService;
import com.rajitha.ecommerce.service.OrderService;
import com.rajitha.ecommerce.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final CustomerClient customerClient;
    private final ProductClient productClient ;
    private final OrderRepository orderRepository;
    private final OrderLineService orderLineService;
    private final OrderProducer orderProducer;

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


//        send the order conform  --> notification-ms(kafka)
        orderProducer.sendOrderConformation( OrderConformationDTO.builder()
                        .orderReference(orderRequestDTO.reference())
                        .customerResponseDTO(customer)
                        .totalAmount(orderRequestDTO.totalAmount())
                        .paymentMethode(orderRequestDTO.paymentMethode())
                        .products(purchaseProduct)

                .build()
        );
        return order.getId();
    }
}
