package com.project.upbit_clone.trade.application.engine;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.orderbook.BookOrderEntry;
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
        // taker 비었거나, 매칭되는 가격없을경우 handleNoMatch 반환.
        if (bestOppositeLevel.isEmpty() || !isPriceCrossed(message, bestOppositeLevel.get().price())) {
            return handleNoMatch(message, orderBook);
        }

        throw new EngineException("실제 매칭 루프는 아직 구현되지 않았습니다.");
    }

    // maker 최적가격 찾기.
    private Optional<PriceLevel.Snapshot> findBestOppositeLevel(
            CommandMessage.Place message,
            InMemoryOrderBook orderBook
    ) {
        return message.orderSide() == OrderSide.BID
                ? orderBook.getBestAsk()
                : orderBook.getBestBid();
    }

    private boolean isPriceCrossed(CommandMessage.Place message, BigDecimal oppositeBestPrice) {
        // 시장은 항상 price cross 체크 true (가격 지정 없으니)
        if (message.orderType() == OrderType.MARKET) {
            return true;
        }

        return message.orderSide() == OrderSide.BID
                // 매수면 구매가 보다 best price가 더 싸야함.
                ? message.price().compareTo(oppositeBestPrice) >= 0
                // 매도면 판매가 보다 best price가 더 비싸야함.
                : message.price().compareTo(oppositeBestPrice) <= 0;
    }

    // 전량 미체결
    private EngineResult.PlaceResult handleNoMatch(
            CommandMessage.Place message,
            InMemoryOrderBook orderBook
    ) {
        if (message.orderType() == OrderType.LIMIT) {
            return handleNoMatchLimit(message, orderBook);
        }

        // 시장가는 IOC라 바로 취소.
        BigDecimal remainingQuantity = message.orderSide() == OrderSide.ASK ? message.quantity() : null;
        BigDecimal unlockAmount = message.orderSide() == OrderSide.BID
                ? message.quoteAmount()
                : message.quantity();

        return EngineResult.PlaceResult.canceled(
                remainingQuantity,
                unlockAmount,
                EngineResult.CancelReason.NO_TRADE_STREAM
        );
    }

    // 지정가 미체결은 오더북에 남긴다.
    private EngineResult.PlaceResult handleNoMatchLimit(
            CommandMessage.Place message,
            InMemoryOrderBook orderBook
    ) {
        BookOrderEntry restingEntry = createRestingEntry(message);
        EngineResult.BookDelta bookDelta = createRestingOrderAddedDelta(orderBook, restingEntry);
        EngineResult.PlaceResult result = EngineResult.PlaceResult.open(
                message.quantity(),
                java.util.List.of(bookDelta)
        );

        orderBook.add(restingEntry);

        return result;
    }

    // 오더북 delta 객체 생성.
    private EngineResult.BookDelta createRestingOrderAddedDelta(
            InMemoryOrderBook orderBook,
            BookOrderEntry restingEntry
    ) {
        InMemoryOrderBook.LevelDelta levelDelta = orderBook.previewAdd(restingEntry);

        return new EngineResult.BookDelta(
                levelDelta,
                EngineResult.BookDeltaReason.RESTING_ORDER_ADDED
        );
    }

    // 오더북 엔트리 생성.
    private BookOrderEntry createRestingEntry(CommandMessage.Place message) {
        return BookOrderEntry.create(
                message.commandLogId(),
                message.orderSide(),
                message.price(),
                message.quantity()
        );
    }
}
