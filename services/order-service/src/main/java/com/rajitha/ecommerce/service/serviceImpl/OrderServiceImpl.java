package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.OrderRequestDTO;
import com.rajitha.ecommerce.serviceImpl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderServiceImpl {

    private final CustomerClient customerClient;
    @Override
    public Integer createOrder(OrderRequestDTO orderRequestDTO) {
//        check the customer -> OpenFeign


//        purchase the products ->product-ms

//        persist oder
//        persist order line
//        Start payment process
//        send the order conform  --> notification-ms(kafka)

        return 0;
    }
}
