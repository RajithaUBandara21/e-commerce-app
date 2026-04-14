package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.dto.OrderLineRequestDTO;
import com.rajitha.ecommerce.mapper.OrderLineMapper;
import com.rajitha.ecommerce.repository.OrderLineRepository;
import com.rajitha.ecommerce.service.OrderLineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderLineServiceImpl implements OrderLineService {

    private final OrderLineRepository orderLineRepository;
    private final OrderLineMapper orderLineMapper;
    @Override
    public Integer saveOrderLine(OrderLineRequestDTO orderLineRequest) {
    var order =  orderLineMapper.toOrderLine(orderLineRequest);
    return 1;
    }
}
