package com.rajitha.ecommerce.service.serviceImpl;

import com.rajitha.ecommerce.dto.OrderLineFulfillmentRequestDTO;
import com.rajitha.ecommerce.dto.OrderLineRequestDTO;
import com.rajitha.ecommerce.dto.OrderLineResponseDTO;
import com.rajitha.ecommerce.entity.Order;
import com.rajitha.ecommerce.entity.OrderLine;
import com.rajitha.ecommerce.enums.OrderLineStatus;
import com.rajitha.ecommerce.enums.OrderStatus;
import com.rajitha.ecommerce.exception.BusinessException;
import com.rajitha.ecommerce.exception.OrderAccessDeniedException;
import com.rajitha.ecommerce.mapper.OrderLineMapper;
import com.rajitha.ecommerce.repository.OrderLineRepository;
import com.rajitha.ecommerce.repository.OrderRepository;
import com.rajitha.ecommerce.service.OrderLineService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderLineServiceImpl implements OrderLineService {

    // Fulfillment only makes sense once payment has actually gone through, and
    // shouldn't be touched once an order is refunded.
    private static final EnumSet<OrderStatus> FULFILLABLE_STATUSES =
            EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED);

    // What counts as an actually-paid-for purchase for review eligibility —
    // excludes PENDING_PAYMENT/PAYMENT_FAILED/CANCELLED/REFUNDED.
    private static final List<OrderStatus> PURCHASED_STATUSES =
            List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;
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

    @Override
    public List<OrderLineResponseDTO> findBySellerId(String sellerId) {
        return orderLineRepository.findBySellerId(sellerId)
                .stream()
                .map(orderLineMapper::toOrderLineResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderLineResponseDTO updateFulfillment(Integer orderLineId, OrderLineFulfillmentRequestDTO request, String callerId, boolean isAdmin) {
        var line = orderLineRepository.findById(orderLineId)
                .orElseThrow(() -> new EntityNotFoundException("Order line not found: " + orderLineId));

        if (!isAdmin && !callerId.equals(line.getSellerId())) {
            throw new OrderAccessDeniedException("You do not own order line " + orderLineId);
        }

        var order = orderRepository.findById(line.getOrder().getId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found for line " + orderLineId));

        if (!FULFILLABLE_STATUSES.contains(order.getStatus())) {
            throw new BusinessException("Order " + order.getId() + " is " + order.getStatus() + " — cannot update fulfillment");
        }

        if (request.status().ordinal() < line.getStatus().ordinal()) {
            throw new BusinessException("Cannot move fulfillment status backward from " + line.getStatus() + " to " + request.status());
        }

        line.setStatus(request.status());
        if (request.trackingNumber() != null && !request.trackingNumber().isBlank()) {
            line.setTrackingNumber(request.trackingNumber());
        }
        orderLineRepository.save(line);

        recomputeOrderStatus(order);

        return orderLineMapper.toOrderLineResponseDTO(line);
    }

    @Override
    public List<Integer> findPurchasedVariantIds(String customerId) {
        return orderLineRepository.findByOrder_CustomerIdAndOrder_StatusIn(customerId, PURCHASED_STATUSES)
                .stream()
                .map(OrderLine::getVariantId)
                .distinct()
                .collect(Collectors.toList());
    }

    private void recomputeOrderStatus(Order order) {
        var lines = orderLineRepository.findOrderLinesByOrderId(order.getId());
        if (lines.isEmpty()) {
            return;
        }

        boolean allDelivered = lines.stream().allMatch(l -> l.getStatus() == OrderLineStatus.DELIVERED);
        boolean anyShippedOrDelivered = lines.stream()
                .anyMatch(l -> l.getStatus() == OrderLineStatus.SHIPPED || l.getStatus() == OrderLineStatus.DELIVERED);

        if (allDelivered && order.getStatus() != OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
        } else if (anyShippedOrDelivered && order.getStatus() != OrderStatus.SHIPPED && order.getStatus() != OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.SHIPPED);
            orderRepository.save(order);
        }
    }
}
