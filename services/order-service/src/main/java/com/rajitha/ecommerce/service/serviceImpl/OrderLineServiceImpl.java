package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.dto.OrderLineRequestDTO;
import com.rajitha.ecommerce.dto.OrderLineResponseDTO;
import com.rajitha.ecommerce.mapper.OrderLineMapper;
import com.rajitha.ecommerce.repository.OrderLineRepository;
import com.rajitha.ecommerce.service.OrderLineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderLineServiceImpl implements OrderLineService {

    private final OrderLineRepository orderLineRepository;
    private final OrderLineMapper orderLineMapper;


    @Override
    public Integer saveOrderLine(OrderLineRequestDTO orderLineRequest) {

    var order =  orderLineRepository.save(orderLineMapper.toOrderLine(orderLineRequest));
    return order.getId();
    }


    @Override
    public List<OrderLineResponseDTO> findOrderLineByOrderId(Integer orderId) {
        return orderLineRepository.findOrderLinesByOrderId(orderId)
                .stream()
                .map(orderLineMapper::toOrderLineResponseDTO)
                .collect(Collectors.toList());
    }
}
