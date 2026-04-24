package com.project.upbit_clone.trade.presentation.response;

import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.infrastructure.persistence.model.OrderBookProjection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderBookResponse(
        Long marketId,
        List<Level> bids,
        List<Level> asks
) {

    public static OrderBookResponse from(
            Long marketId,
            List<OrderBookProjection> bids,
            List<OrderBookProjection> asks
    ) {
        return new OrderBookResponse(
                marketId,
                bids.stream().map(Level::from).toList(),
                asks.stream().map(Level::from).toList()
        );
    }

    public record Level(
            OrderSide side,
            BigDecimal price,
            BigDecimal totalQty,
            Integer orderCount,
            LocalDateTime updatedAt
    ) {

        public static Level from(OrderBookProjection projection) {
            return new Level(
                    projection.getId().getSide(),
                    projection.getId().getPrice(),
                    projection.getTotalQty(),
                    projection.getOrderCount(),
                    projection.getUpdatedAt()
            );
        }
    }
}
