package com.project.upbit_clone.trade.domain.repository;

import com.project.upbit_clone.trade.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUser_IdAndMarket_IdAndClientOrderId(Long userId, Long marketId, String clientOrderId);
}
