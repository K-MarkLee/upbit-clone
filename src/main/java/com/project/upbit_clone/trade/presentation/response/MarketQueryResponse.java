package com.project.upbit_clone.trade.presentation.response;

import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.trade.domain.model.Market;

import java.math.BigDecimal;

public record MarketQueryResponse(
        Long marketId,
        String marketCode,
        EnumStatus status,
        BigDecimal minOrderQuote,
        BigDecimal tickSize,
        AssetInfo baseAsset,
        AssetInfo quoteAsset
) {

    public static MarketQueryResponse from(Market market) {
        return new MarketQueryResponse(
                market.getId(),
                market.getMarketCode(),
                market.getStatus(),
                market.getMinOrderQuote(),
                market.getTickSize(),
                AssetInfo.from(market.getBaseAsset().getSymbol(), market.getBaseAsset().getName()),
                AssetInfo.from(market.getQuoteAsset().getSymbol(), market.getQuoteAsset().getName())
        );
    }

    public record AssetInfo(
            String symbol,
            String name
    ) {

        private static AssetInfo from(String symbol, String name) {
            return new AssetInfo(symbol, name);
        }
    }
}
