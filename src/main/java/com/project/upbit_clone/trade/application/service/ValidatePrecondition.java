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

        // 커맨드 null / blank 검증
        validateCommand(command);

        // 조회 및 검증
        validateActiveUser(command.userId());
        validateActiveMarket(command.marketId());

        return findExistingOrder(command.userId(), command.clientOrderId());
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

    // 사전조건 커맨드의 필수값 null / blank값을 검증한다.
    private static void validateCommand(OrderOrchestrator.PlaceOrderCommand command) {
        if (command == null
                || command.userId() == null
                || command.marketId() == null
                || command.side() == null
                || command.clientOrderId() == null
                || command.clientOrderId().isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_VALUE);
        }
    }

}
