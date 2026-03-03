package com.project.upbit_clone.trade.infrastructure.persistence.repository;

import com.project.upbit_clone.trade.infrastructure.persistence.model.OrderBookProjection;
import com.project.upbit_clone.trade.infrastructure.persistence.model.OrderBookProjectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OrderBookProjectionRepository extends JpaRepository<OrderBookProjection, OrderBookProjectionId> {
}
