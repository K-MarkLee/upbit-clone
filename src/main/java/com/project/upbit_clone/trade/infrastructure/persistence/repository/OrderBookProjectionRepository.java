package com.project.upbit_clone.trade.infrastructure.persistence.repository;

import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.infrastructure.persistence.model.OrderBookProjection;
import com.project.upbit_clone.trade.infrastructure.persistence.model.OrderBookProjectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderBookProjectionRepository extends JpaRepository<OrderBookProjection, OrderBookProjectionId> {

    List<OrderBookProjection> findTop30ByIdMarketIdAndIdSideOrderByIdPriceAsc(Long marketId, OrderSide side);

    List<OrderBookProjection> findTop30ByIdMarketIdAndIdSideOrderByIdPriceDesc(Long marketId, OrderSide side);
}
