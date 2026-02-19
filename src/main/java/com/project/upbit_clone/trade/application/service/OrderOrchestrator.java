package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderOrchestrator {

    private final ValidatePrecondition validatePrecondition;
    private final CreateOrder createOrder;
    private final MatchingService matchingService;

    public Order placeOrder(PlaceOrderCommand command) {
        Optional<Order> existingOrder = validatePrecondition.validateOrderPreconditions(command);
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        Order createdOrder = createOrder.createOrder(command);
        matchingService.match(createdOrder.getId());
        return createdOrder;
    }

    public record PlaceOrderCommand(
            Long userId,
            Long marketId,
            OrderSide side,
            OrderType orderType,
            TimeInForce timeInForce,
            String clientOrderId,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount
    ) {
    }
}
