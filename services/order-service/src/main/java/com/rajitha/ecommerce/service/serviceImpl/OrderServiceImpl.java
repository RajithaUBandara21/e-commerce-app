package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.client.feign.CustomerClient;
import com.rajitha.ecommerce.client.rest.ProductClient;
import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
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

    @Override
    public Integer createOrder(OrderRequestDTO orderRequestDTO) {
//        check the customer -> OpenFeign
        var customer  = customerClient.findCustomerById(orderRequestDTO.customerId()).orElseThrow(()->new BusinessException ("Cannot create order :: no customer exists with provided id: " + orderRequestDTO.customerId() ));

//        purchase the products ->product-ms (use Rest template)
        this.productClient.purchaseProducts(orderRequestDTO.products());

//        persist oder
        var order = orderRepository.save(orderMapper.toOder(orderRequestDTO));

//        persist order line
        for(PurchaseRequestDTO purchaseRequestDTO : orderRequestDTO.products()){
            orderLineService.saveOrderLine(

            )
        }
//        Start payment process
//        send the order conform  --> notification-ms(kafka)

        return 0;
    }
}
