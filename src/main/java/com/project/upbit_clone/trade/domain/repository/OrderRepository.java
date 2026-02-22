package com.project.upbit_clone.trade.domain.repository;

import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserIdAndClientOrderId(Long userId, String clientOrderId);

    // 동일 주문 재매칭 요청이 동시에 들어올 때, taker 주문 단위로 직렬화한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByIdAndStatus(Long orderId, OrderStatus status);

    // maker BID: 최고가 우선, 동일가면 먼저 들어온 주문 우선
    Sort bidsort = Sort.by(Sort.Direction.DESC, "price")
            .and(Sort.by(Sort.Direction.ASC, "createdAt"));

    // maker ASK: 최저가 우선, 동일가면 먼저 들어온 주문 우선
    Sort asksort = Sort.by(Sort.Direction.ASC, "price")
            .and(Sort.by(Sort.Direction.ASC, "createdAt"));

    Optional<Order> findFirstByMarketIdAndStatusAndOrderSideAndOrderTypeAndPriceIsNotNullAndUserIdNot(
            Long marketId,
            OrderStatus status,
            OrderSide orderSide,
            OrderType orderType,
            Long takerUserId,
            Sort sort
    );


}
