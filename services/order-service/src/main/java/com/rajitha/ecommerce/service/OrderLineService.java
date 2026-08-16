package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.OrderLineFulfillmentRequestDTO;
import com.rajitha.ecommerce.dto.OrderLineRequestDTO;
import com.rajitha.ecommerce.dto.OrderLineResponseDTO;

import java.util.List;

public interface OrderLineService {

    Integer saveOrderLine(OrderLineRequestDTO orderLineRequest);

    List<OrderLineResponseDTO> findOrderLineByOrderId(Integer orderId);

    List<OrderLineResponseDTO> findBySellerId(String sellerId);

    OrderLineResponseDTO updateFulfillment(Integer orderLineId, OrderLineFulfillmentRequestDTO request, String callerId, boolean isAdmin);

    List<Integer> findPurchasedVariantIds(String customerId);
}
