package com.project.upbit_clone.trade.domain.repository;

import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserIdAndClientOrderId(Long userId, String clientOrderId);

    // maker BID: 최고가 우선, 동일가면 먼저 들어온 주문 우선
    Sort bidsort = Sort.by(Sort.Direction.DESC, "price")
            .and(Sort.by(Sort.Direction.ASC, "createdAt"));

    // maker ASK: 최저가 우선, 동일가면 먼저 들어온 주문 우선
    Sort asksort = Sort.by(Sort.Direction.ASC, "price")
            .and(Sort.by(Sort.Direction.ASC, "createdAt"));

    Optional<Order> findFirstByMarketIdAndStatusAndOrderSideAndUserIdNot(
            Long marketId,
            OrderStatus status,
            OrderSide orderSide,
            Long takerUserId,
            Sort sort
    );


}
