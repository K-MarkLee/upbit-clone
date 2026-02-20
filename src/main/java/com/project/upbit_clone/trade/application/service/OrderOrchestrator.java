package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderOrchestrator {

    private final ValidatePrecondition validatePrecondition;
    private final CreateOrder createOrder;
    private final MatchingService matchingService;
    private final OrderRepository orderRepository;

    public Order placeOrder(PlaceOrderCommand command) {

        // 주문전 사전정보 검증
        Optional<Order> existingOrder = validatePrecondition.validateOrderPreconditions(command);
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        // 주문 생성 및 유니크 충돌시 재조회후 반환
        Order createdOrder;
        try {
            createdOrder = createOrder.createOrder(command);
        } catch (DataIntegrityViolationException exception) {
            Optional<Order> duplicatedOrder = orderRepository.findByUserIdAndClientOrderId(
                    command.userId(),
                    command.clientOrderId()
            );
            if (duplicatedOrder.isPresent()) {
                return duplicatedOrder.get();
            }
            throw exception;
        }

        // 매칭 엔진
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
