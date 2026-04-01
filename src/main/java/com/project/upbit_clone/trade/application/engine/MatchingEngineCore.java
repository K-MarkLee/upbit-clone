package com.project.upbit_clone.trade.application.engine;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.orderbook.BookOrderEntry;
import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.application.engine.orderbook.PriceLevel;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class MatchingEngineCore {

    public EngineResult.PlaceResult place(CommandMessage.Place message, InMemoryOrderBook orderBook) {
        Objects.requireNonNull(message, "message는 null일 수 없습니다.");
        Objects.requireNonNull(orderBook, "orderBook은 null일 수 없습니다.");

        Optional<BookOrderEntry> bestOppositeHead = findBestOppositeHead(message, orderBook);
        // taker 비었거나, 매칭되는 가격없을경우 handleNoMatch 반환.
        if (bestOppositeHead.isEmpty() || !isPriceCrossed(message, bestOppositeHead.get().getPrice())) {
            return handleNoMatch(message, orderBook);
        }

        return handleSingleMakerFilled(message, bestOppositeHead.get(), orderBook);
    }

    // maker 최적가격의 선두 주문 찾기.
    private Optional<BookOrderEntry> findBestOppositeHead(
            CommandMessage.Place message,
            InMemoryOrderBook orderBook
    ) {
        return message.orderSide() == OrderSide.BID
                ? orderBook.getBestAskHead()
                : orderBook.getBestBidHead();
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

    // 단일 maker 전량 체결
    private EngineResult.PlaceResult handleSingleMakerFilled(
            CommandMessage.Place message,
            BookOrderEntry makerHead,
            InMemoryOrderBook orderBook
    ) {
        // executed 계산
        BigDecimal takerRemainingQuantity = requireTakerQuantity(message);
        BigDecimal executedQuantity = takerRemainingQuantity.min(makerHead.getRemainingQty());
        BigDecimal executedQuoteAmount = makerHead.getPrice().multiply(executedQuantity);
        BigDecimal makerRemainingQuantityAfter = makerHead.getRemainingQty().subtract(executedQuantity);
        BigDecimal takerRemainingQuantityAfter = takerRemainingQuantity.subtract(executedQuantity);

        if (takerRemainingQuantityAfter.compareTo(BigDecimal.ZERO) > 0) {
            throw new EngineException("단일 maker 전량체결(S2) 범위를 벗어났습니다.");
        }

        // before price level 찾기
        PriceLevel.Snapshot before = orderBook.getLevelSnapshot(makerHead.getSide(), makerHead.getPrice())
                .orElseThrow(() -> new EngineException("maker price level snapshot을 찾을 수 없습니다."));

        EngineResult.Fill fill = new EngineResult.Fill(
                makerHead.getOrderId(),
                makerHead.getPrice(),
                executedQuantity,
                executedQuoteAmount,
                makerRemainingQuantityAfter
        );

        EngineResult.BookDelta bookDelta = createMatchExecutedDelta(
                before,
                makerHead,
                executedQuantity,
                makerRemainingQuantityAfter
        );

        EngineResult.PlaceResult result = EngineResult.PlaceResult.filled(
                executedQuantity,
                executedQuoteAmount,
                List.of(fill),
                List.of(bookDelta)
        );

        orderBook.applyExecution(makerHead.getSide(), makerHead.getPrice(), executedQuantity);
        return result;
    }

    private BigDecimal requireTakerQuantity(CommandMessage.Place message) {
        if (message.quantity() == null) {
            throw new EngineException("현재 S2는 quantity 기반 주문만 지원합니다.");
        }
        return message.quantity();
    }

    // 북델타 생성
    private EngineResult.BookDelta createMatchExecutedDelta(
            PriceLevel.Snapshot before,
            BookOrderEntry makerHead,
            BigDecimal executedQuantity,
            BigDecimal makerRemainingQuantityAfter
    ) {
        return new EngineResult.BookDelta(
                new InMemoryOrderBook.LevelDelta(
                        makerHead.getSide(),
                        makerHead.getPrice(),
                        before,
                        createAfterMatchSnapshot(before, makerHead, executedQuantity, makerRemainingQuantityAfter)
                ),
                EngineResult.BookDeltaReason.MATCH_EXECUTED
        );
    }

    // after level delta생성
    private PriceLevel.Snapshot createAfterMatchSnapshot(
            PriceLevel.Snapshot before,
            BookOrderEntry makerHead,
            BigDecimal executedQuantity,
            BigDecimal makerRemainingQuantityAfter
    ) {
        if (makerRemainingQuantityAfter.compareTo(BigDecimal.ZERO) == 0 && before.orderCount() == 1) {
            return PriceLevel.emptySnapshot(makerHead.getSide(), makerHead.getPrice());
        }

        return new PriceLevel.Snapshot(
                makerHead.getSide(),
                makerHead.getPrice(),
                before.totalQty().subtract(executedQuantity),
                makerRemainingQuantityAfter.compareTo(BigDecimal.ZERO) == 0
                        ? before.orderCount() - 1
                        : before.orderCount()
        );
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
                List.of(bookDelta)
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
