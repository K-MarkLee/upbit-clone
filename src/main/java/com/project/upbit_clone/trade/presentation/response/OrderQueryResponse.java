package com.project.upbit_clone.trade.presentation.response;

import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderQueryResponse(
        Long marketId,
        String marketCode,
        String clientOrderId,
        OrderSide orderSide,
        OrderType orderType,
        TimeInForce timeInForce,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal quoteAmount,
        BigDecimal executedQuantity,
        BigDecimal executedQuoteAmount,
        OrderStatus status,
        String cancelReason,
        String baseAssetSymbol,
        String quoteAssetSymbol,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OrderQueryResponse from(Order order) {
        return new OrderQueryResponse(
                order.getMarket().getId(),
                order.getMarket().getMarketCode(),
                order.getClientOrderId(),
                order.getOrderSide(),
                order.getOrderType(),
                order.getTimeInForce(),
                order.getPrice(),
                order.getQuantity(),
                order.getQuoteAmount(),
                order.getExecutedQuantity(),
                order.getExecutedQuoteAmount(),
                order.getStatus(),
                order.getCancelReason(),
                order.getMarket().getBaseAsset().getSymbol(),
                order.getMarket().getQuoteAsset().getSymbol(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
