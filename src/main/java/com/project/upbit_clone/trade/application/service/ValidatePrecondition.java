package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ValidatePrecondition {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;

    /*
    * 주문 생성 전 사전 조건
    * 사용자 매칭 / 시장과 자산의 매칭
    * */
    @Transactional(readOnly = true)
    public Optional<Order> validateOrderPreconditions(OrderOrchestrator.PlaceOrderCommand command) {

        // 멱등성 키 최소 검증 후, 멱등성 히트를 먼저 확인한다.
        validateIdempotencyInput(command);
        Optional<Order> existingOrder = findExistingOrder(command.userId(), command.clientOrderId());
        if (existingOrder.isPresent()) {
            return existingOrder;
        }

        // 신규 주문 생성 경로에서만 사전조건을 검증한다.
        validateCreatePreconditions(command);
        return Optional.empty();
    }

    // 주문을 요청한 사용자를 조회하고, 주문이 가능한 상태인지 검증한다.
    private void validateActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != EnumStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_ALLOWED_USER_STATUS);
        }
    }

    // 주문이 요청된 시장을 조회하고, 거래 가능한 상태인지 검증한다.
    private void validateActiveMarket(Long marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MARKET_NOT_FOUND));

        if (market.getStatus() != EnumStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_ALLOWED_MARKET_STATUS);
        }
    }

    // 동일한 clientOrderId로 접수된 기존의 주문을 조회한다. (멱등성 히트)
    private Optional<Order> findExistingOrder(Long userId, String clientOrderId) {
        return orderRepository.findByUserIdAndClientOrderId(userId, clientOrderId);
    }

    // 멱등성 조회를 위해 필요한 최소 입력을 검증한다. (최소 값)
    private static void validateIdempotencyInput(OrderOrchestrator.PlaceOrderCommand command) {
        if (command == null
                || command.userId() == null
                || command.clientOrderId() == null
                || command.clientOrderId().isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_ORDER_REQUIRED_VALUE);
        }
    }

    // 신규 주문 생성 경로에서 필요한 사전조건을 검증한다.
    private void validateCreatePreconditions(OrderOrchestrator.PlaceOrderCommand command) {
        if (command.marketId() == null || command.side() == null) {
            throw new BusinessException(ErrorCode.MISSING_ORDER_REQUIRED_VALUE);
        }

        validateActiveUser(command.userId());
        validateActiveMarket(command.marketId());
    }

}
