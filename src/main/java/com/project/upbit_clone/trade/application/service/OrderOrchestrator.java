package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
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
    private final MatchingRetryService matchingRetryService;
    private final OrderRepository orderRepository;

    public Order placeOrder(PlaceOrderCommand command) {

        // 주문전 사전정보 검증
        Optional<Order> existingOrder = validatePrecondition.validateOrderPreconditions(command);
        if (existingOrder.isPresent()) {
            return retryMatchingAndReload(existingOrder.get());
        }

        // 주문 생성 및 유니크 충돌시 재조회후 반환
        // TODO : 재매칭시 이전 주문 매칭중인지 검증 필요.
        Order createdOrder;
        try {
            createdOrder = createOrder.createOrder(command);
        } catch (DataIntegrityViolationException exception) {
            Optional<Order> duplicatedOrder = orderRepository.findByUserIdAndClientOrderId(
                    command.userId(),
                    command.clientOrderId()
            );
            if (duplicatedOrder.isPresent()) {
                return retryMatchingAndReload(duplicatedOrder.get());
            }
            throw exception;
        }

        // 매칭 엔진
        matchingRetryService.matchWithRetry(createdOrder.getId());
        return reloadOrder(createdOrder.getId());
    }

    // OPEN 주문은 매칭을 재시도하고, 항상 최신 상태로 재조회해서 반환한다.
    private Order retryMatchingAndReload(Order order) {
        if (order.getStatus() == OrderStatus.OPEN) {
            validatePrecondition.validateActiveUserAndMarket(
                    order.getUser().getId(),
                    order.getMarket().getId()
            );
            matchingRetryService.matchWithRetry(order.getId());
        }
        return reloadOrder(order.getId());
    }
    private Order reloadOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
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
