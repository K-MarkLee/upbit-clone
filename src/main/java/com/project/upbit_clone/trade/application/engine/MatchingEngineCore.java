package com.project.upbit_clone.trade.application.engine;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.application.engine.orderbook.PriceLevel;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Component
public class MatchingEngineCore {

    public EngineResult.PlaceResult place(CommandMessage.Place message, InMemoryOrderBook orderBook) {
        Objects.requireNonNull(message, "message는 null일 수 없습니다.");
        Objects.requireNonNull(orderBook, "orderBook은 null일 수 없습니다.");

        Optional<PriceLevel.Snapshot> bestOppositeLevel = findBestOppositeLevel(message, orderBook);
        if (bestOppositeLevel.isEmpty() || !isPriceCrossed(message, bestOppositeLevel.get().price())) {
            return handleNoMatch(message);
        }

        throw new EngineException("실제 매칭 루프는 아직 구현되지 않았습니다.");
    }

    private Optional<PriceLevel.Snapshot> findBestOppositeLevel(
            CommandMessage.Place message,
            InMemoryOrderBook orderBook
    ) {
        return message.orderSide() == OrderSide.BID
                ? orderBook.getBestAsk()
                : orderBook.getBestBid();
    }

    private boolean isPriceCrossed(CommandMessage.Place message, BigDecimal oppositeBestPrice) {
        if (message.orderType() == OrderType.MARKET) {
            return true;
        }

        return message.orderSide() == OrderSide.BID
                ? message.price().compareTo(oppositeBestPrice) >= 0
                : message.price().compareTo(oppositeBestPrice) <= 0;
    }

    private EngineResult.PlaceResult handleNoMatch(CommandMessage.Place message) {
        if (message.orderType() == OrderType.LIMIT) {
            return EngineResult.PlaceResult.open(message.quantity());
        }

        BigDecimal remainingQuantity = message.orderSide() == OrderSide.ASK ? message.quantity() : null;
        BigDecimal unlockAmount = message.orderSide() == OrderSide.BID
                ? message.quoteAmount()
                : message.quantity();
        return EngineResult.PlaceResult.canceled(
                remainingQuantity,
                unlockAmount,
                "NO_TRADE_STREAM"
        );
    }
}
