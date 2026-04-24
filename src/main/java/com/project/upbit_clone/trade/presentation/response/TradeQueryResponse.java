package com.project.upbit_clone.trade.presentation.response;

import com.project.upbit_clone.trade.domain.model.Trade;
import com.project.upbit_clone.trade.domain.vo.OrderSide;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeQueryResponse(
        Long tradeId,
        Long marketId,
        String marketCode,
        OrderSide makerOrderSide,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal quoteAmount,
        LocalDateTime executedAt
) {

    public static TradeQueryResponse from(Trade trade) {
        return new TradeQueryResponse(
                trade.getId(),
                trade.getMarket().getId(),
                trade.getMarket().getMarketCode(),
                trade.getMakerOrderSide(),
                trade.getPrice(),
                trade.getQuantity(),
                trade.getQuoteAmount(),
                trade.getExecutedAt()
        );
    }
}
