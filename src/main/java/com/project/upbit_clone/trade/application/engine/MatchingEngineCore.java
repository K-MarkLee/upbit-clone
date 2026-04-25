package com.project.upbit_clone.trade.application.engine;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.orderbook.BookOrderEntry;
import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class MatchingEngineCore {

    public EngineResult.PlaceResult place(CommandMessage.Place message, InMemoryOrderBook orderBook) {
        Objects.requireNonNull(message, "message는 null일 수 없습니다.");
        Objects.requireNonNull(orderBook, "orderBook은 null일 수 없습니다.");

        // 체결 엔진 구분 (limit, market-ask => quantity / market-bid => quote)
        MatchingMode mode = determineMode(message);
        return switch (mode) {
            case QUANTITY_BASED -> placeQuantityBased(message, orderBook);
            case QUOTE_BASED -> placeQuoteBased(message, orderBook);
        };
    }

    private MatchingMode determineMode(CommandMessage.Place message) {
        return (message.orderType() == OrderType.MARKET && message.orderSide() == OrderSide.BID)
                ? MatchingMode.QUOTE_BASED
                : MatchingMode.QUANTITY_BASED;
    }

    private EngineResult.PlaceResult placeQuantityBased(CommandMessage.Place message, InMemoryOrderBook orderBook) {
        // quantity 루프 시작
        QuantityLoopResult loopResult = runQuantityLoop(message, orderBook);
        return finalizeQuantityResult(message, loopResult, orderBook);
    }

    // quantity 루프
    private QuantityLoopResult runQuantityLoop(CommandMessage.Place message, InMemoryOrderBook orderBook) {
        // 초기화
        BigDecimal remainingQuantity = requireQuantity(message);
        BigDecimal totalExecutedQuantity = BigDecimal.ZERO;
        BigDecimal totalExecutedQuoteAmount = BigDecimal.ZERO;
        List<EngineResult.Fill> fills = new ArrayList<>();
        List<EngineResult.BookDelta> bookDeltas = new ArrayList<>();
        StopReason stopReason = StopReason.NO_OPPOSITE;

        while (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            Optional<BookOrderEntry> bestOppositeHead = findBestOppositeHead(message, orderBook);
            // orderbook 비었으면 break
            if (bestOppositeHead.isEmpty()) {
                break;
            }

            // 매칭되는 금액 없으면 break
            BookOrderEntry makerHead = bestOppositeHead.get();
            if (!isPriceCrossed(message, makerHead.getPrice())) {
                stopReason = StopReason.NOT_CROSSED;
                break;
            }
            if (isSelfTrade(message, makerHead)) {
                stopReason = StopReason.SELF_TRADE_BLOCKED;
                break;
            }

            // executed 계산.
            // executedQuantity = 체결 수량 (taker의 남은수량과 maker의 남은수량 중 낮은값)
            // executedQuoteAmount = 체결 금액 (maker의 가격과 체결된 갯수의 곱)
            // makerRemainingQuantityAfter = maker 남은 수량 (maker가 체결 후 남은 수량)
            BigDecimal executedQuantity = remainingQuantity.min(makerHead.getRemainingQty());
            BigDecimal executedQuoteAmount = roundDownQuote(makerHead.getPrice().multiply(executedQuantity), message);
            BigDecimal makerRemainingQuantityAfter = makerHead.getRemainingQty().subtract(executedQuantity);

            if (executedQuoteAmount.compareTo(BigDecimal.ZERO) == 0) {
                stopReason = StopReason.NO_EXECUTABLE_SIZE;
                break;
            }

            // 1회 체결에 대한 정보
            fills.add(new EngineResult.Fill(
                    makerHead.getOrderKey(),
                    makerHead.getPrice(),
                    executedQuantity,
                    executedQuoteAmount,
                    makerRemainingQuantityAfter
            ));
            // book delta 생성 및 order book 업데이트
            bookDeltas.add(createMatchExecutedDelta(
                    orderBook.applyExecution(makerHead.getSide(), makerHead.getPrice(), executedQuantity)
            ));

            // 최종 executed 값 계산
            totalExecutedQuantity = totalExecutedQuantity.add(executedQuantity);
            totalExecutedQuoteAmount = totalExecutedQuoteAmount.add(executedQuoteAmount);
            remainingQuantity = remainingQuantity.subtract(executedQuantity);

            // taker의 남은 수량이 끝나면 종료
            if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
                stopReason = StopReason.TAKER_FILLED;
                break;
            }
        }

        return new QuantityLoopResult(
                totalExecutedQuantity,
                totalExecutedQuoteAmount,
                remainingQuantity,
                List.copyOf(fills),
                List.copyOf(bookDeltas),
                stopReason
        );
    }

    // quantity loop 최종 결과 확정
    private EngineResult.PlaceResult finalizeQuantityResult(
            CommandMessage.Place message,
            QuantityLoopResult loopResult,
            InMemoryOrderBook orderBook
    ) {
        if (loopResult.stopReason() == StopReason.TAKER_FILLED) {
            return EngineResult.PlaceResult.filled(
                    loopResult.executedQuantity(),
                    loopResult.executedQuoteAmount(),
                    calculateFilledUnlockAmount(message, loopResult.executedQuoteAmount()),
                    loopResult.fills(),
                    loopResult.bookDeltas()
            );
        }

        if (loopResult.stopReason() == StopReason.SELF_TRADE_BLOCKED) {
            return EngineResult.PlaceResult.canceled(
                    loopResult.executedQuantity(),
                    loopResult.executedQuoteAmount(),
                    loopResult.remainingQuantity(),
                    calculateCanceledUnlockAmount(message, loopResult),
                    EngineResult.CancelReason.SELF_TRADE_PREVENTED,
                    loopResult.fills(),
                    loopResult.bookDeltas()
            );
        }

        if (message.orderType() == OrderType.LIMIT) {
            return restQuantityBasedLimitOrder(message, loopResult, orderBook);
        }

        EngineResult.CancelReason cancelReason = resolveNoRestCancelReason(loopResult.stopReason(), loopResult.hasExecution());
        BigDecimal unlockAmount = calculateCanceledUnlockAmount(message, loopResult);
        return EngineResult.PlaceResult.canceled(
                loopResult.executedQuantity(),
                loopResult.executedQuoteAmount(),
                loopResult.remainingQuantity(),
                unlockAmount,
                cancelReason,
                loopResult.fills(),
                loopResult.bookDeltas()
        );
    }

    // limit 최종 처리 (부분 체결 확인후 남은 금액 오더북 업데이트)
    private EngineResult.PlaceResult restQuantityBasedLimitOrder(
            CommandMessage.Place message,
            QuantityLoopResult loopResult,
            InMemoryOrderBook orderBook
    ) {
        // order book 엔트리 생성
        BookOrderEntry restingEntry = BookOrderEntry.create(
                message.orderKey(),
                message.userId(),
                message.orderSide(),
                message.price(),
                loopResult.remainingQuantity()
        );
        // book delta 생성
        EngineResult.BookDelta restingBookDelta = new EngineResult.BookDelta(
                orderBook.previewAdd(restingEntry),
                EngineResult.BookDeltaReason.RESTING_ORDER_ADDED
        );
        orderBook.add(restingEntry);

        List<EngineResult.BookDelta> bookDeltas = new ArrayList<>(loopResult.bookDeltas());
        bookDeltas.add(restingBookDelta);

        // open result반환
        return EngineResult.PlaceResult.open(
                loopResult.executedQuantity(),
                loopResult.executedQuoteAmount(),
                loopResult.remainingQuantity(),
                calculateOpenUnlockAmount(message, loopResult),
                loopResult.fills(),
                List.copyOf(bookDeltas)
        );
    }

    // quote 루프 분기
    private EngineResult.PlaceResult placeQuoteBased(CommandMessage.Place message, InMemoryOrderBook orderBook) {
        // quote 루프 시작
        QuoteLoopResult loopResult = runQuoteLoop(message, orderBook);
        return finalizeQuoteResult(loopResult);
    }

    private QuoteLoopResult runQuoteLoop(CommandMessage.Place message, InMemoryOrderBook orderBook) {
        // 초기화
        BigDecimal remainingQuoteAmount = requireQuoteAmount(message);
        BigDecimal totalExecutedQuantity = BigDecimal.ZERO;
        BigDecimal totalExecutedQuoteAmount = BigDecimal.ZERO;
        List<EngineResult.Fill> fills = new ArrayList<>();
        List<EngineResult.BookDelta> bookDeltas = new ArrayList<>();
        StopReason stopReason = StopReason.NO_OPPOSITE;

        while (remainingQuoteAmount.compareTo(BigDecimal.ZERO) > 0) {
            // opposite 없으면 종료
            Optional<BookOrderEntry> bestOppositeHead = findBestOppositeHead(message, orderBook);
            if (bestOppositeHead.isEmpty()) {
                break;
            }

            BookOrderEntry makerHead = bestOppositeHead.get();
            if (isSelfTrade(message, makerHead)) {
                stopReason = StopReason.SELF_TRADE_BLOCKED;
                break;
            }

            BigDecimal roundDownQu = roundDownQuote(
                    makerHead.getPrice().multiply(makerHead.getRemainingQty()),
                    message
            );

            //executableBudget = min(taker 거래가능 금액, maker 남은 quantity * price)
            BigDecimal executableBudget = remainingQuoteAmount.min(roundDownQu);

            // 거래 가능 금액으로 몇개 살 수 있는지. 금액 / 가격 = 수량
            BigDecimal executedQuantity = executableBudget.divide(
                    makerHead.getPrice(),
                    // 소수점 자리수 기준 내림.
                    message.baseAssetScale(),
                    RoundingMode.DOWN
            );

            // 거래 가능 갯수가 없으면 break.
            if (executedQuantity.compareTo(BigDecimal.ZERO) == 0) {
                stopReason = StopReason.NO_EXECUTABLE_SIZE;
                break;
            }

            // executed 계산
            BigDecimal executedQuoteAmount = roundDownQuote(makerHead.getPrice().multiply(executedQuantity), message);
            BigDecimal makerRemainingQuantityAfter = makerHead.getRemainingQty().subtract(executedQuantity);

            if (executedQuoteAmount.compareTo(remainingQuoteAmount) > 0) {
                stopReason = StopReason.NO_EXECUTABLE_SIZE;
                break;
            }

            fills.add(new EngineResult.Fill(
                    makerHead.getOrderKey(),
                    makerHead.getPrice(),
                    executedQuantity,
                    executedQuoteAmount,
                    makerRemainingQuantityAfter
            ));

            // book delta와 order book 업데이트
            bookDeltas.add(createMatchExecutedDelta(
                    orderBook.applyExecution(makerHead.getSide(), makerHead.getPrice(), executedQuantity)
            ));

            // 최종 executed 계산
            totalExecutedQuantity = totalExecutedQuantity.add(executedQuantity);
            totalExecutedQuoteAmount = totalExecutedQuoteAmount.add(executedQuoteAmount);
            remainingQuoteAmount = remainingQuoteAmount.subtract(executedQuoteAmount);

            // taker filled면 break.
            if (remainingQuoteAmount.compareTo(BigDecimal.ZERO) == 0) {
                stopReason = StopReason.TAKER_FILLED;
                break;
            }
        }

        return new QuoteLoopResult(
                totalExecutedQuantity,
                totalExecutedQuoteAmount,
                remainingQuoteAmount,
                List.copyOf(fills),
                List.copyOf(bookDeltas),
                stopReason
        );
    }

    private EngineResult.PlaceResult finalizeQuoteResult(QuoteLoopResult loopResult) {
        if (loopResult.stopReason() == StopReason.TAKER_FILLED) {
            return EngineResult.PlaceResult.filled(
                    loopResult.executedQuantity(),
                    loopResult.executedQuoteAmount(),
                    BigDecimal.ZERO,
                    loopResult.fills(),
                    loopResult.bookDeltas()
            );
        }

        EngineResult.CancelReason cancelReason = resolveNoRestCancelReason(loopResult.stopReason(), loopResult.hasExecution());
        return EngineResult.PlaceResult.canceled(
                loopResult.executedQuantity(),
                loopResult.executedQuoteAmount(),
                null,
                loopResult.remainingQuoteAmount(),
                cancelReason,
                loopResult.fills(),
                loopResult.bookDeltas()
        );
    }

    private EngineResult.CancelReason resolveNoRestCancelReason(StopReason stopReason, boolean hasExecution) {
        if (stopReason == StopReason.SELF_TRADE_BLOCKED) {
            return EngineResult.CancelReason.SELF_TRADE_PREVENTED;
        }
        if (hasExecution) {
            return EngineResult.CancelReason.IOC_REMAINDER;
        }
        return switch (stopReason) {
            case NO_OPPOSITE -> EngineResult.CancelReason.NO_TRADE_STREAM;
            case NOT_CROSSED, NO_EXECUTABLE_SIZE -> EngineResult.CancelReason.IOC_NOT_MATCHED;
            case TAKER_FILLED -> throw new EngineException("filled 상태는 cancelReason을 가질 수 없습니다.");
            case SELF_TRADE_BLOCKED -> throw new EngineException("self trade 상태는 사전 처리되어야 합니다.");
        };
    }

    private EngineResult.BookDelta createMatchExecutedDelta(InMemoryOrderBook.LevelDelta levelDelta) {
        return new EngineResult.BookDelta(levelDelta, EngineResult.BookDeltaReason.MATCH_EXECUTED);
    }

    private Optional<BookOrderEntry> findBestOppositeHead(CommandMessage.Place message, InMemoryOrderBook orderBook) {
        return message.orderSide() == OrderSide.BID
                ? orderBook.getBestAskHead()
                : orderBook.getBestBidHead();
    }

    private boolean isPriceCrossed(CommandMessage.Place message, BigDecimal oppositeBestPrice) {
        if (message.orderType() == OrderType.MARKET) {
            return true;
        }
        return message.orderSide() == OrderSide.BID
                ? message.price().compareTo(oppositeBestPrice) >= 0
                : message.price().compareTo(oppositeBestPrice) <= 0;
    }

    private boolean isSelfTrade(CommandMessage.Place message, BookOrderEntry makerHead) {
        return makerHead.getUserId().equals(message.userId());
    }

    private BigDecimal requireQuantity(CommandMessage.Place message) {
        if (message.quantity() == null) {
            throw new EngineException("quantity는 null일 수 없습니다.");
        }
        return message.quantity();
    }

    private BigDecimal requireQuoteAmount(CommandMessage.Place message) {
        if (message.quoteAmount() == null) {
            throw new EngineException("quoteAmount는 null일 수 없습니다.");
        }
        return message.quoteAmount();
    }

    private BigDecimal calculateOpenUnlockAmount(CommandMessage.Place message, QuantityLoopResult loopResult) {
        if (message.orderType() == OrderType.LIMIT && message.orderSide() == OrderSide.BID) {
            BigDecimal reservedAmount = reservedQuoteAmount(message);
            BigDecimal remainingReserve = roundDownQuote(message.price().multiply(loopResult.remainingQuantity()), message);
            return nonNegativeUnlock(reservedAmount.subtract(remainingReserve).subtract(loopResult.executedQuoteAmount()));
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateFilledUnlockAmount(CommandMessage.Place message, BigDecimal executedQuoteAmount) {
        if (message.orderType() == OrderType.LIMIT && message.orderSide() == OrderSide.BID) {
            return nonNegativeUnlock(reservedQuoteAmount(message).subtract(executedQuoteAmount));
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateCanceledUnlockAmount(CommandMessage.Place message, QuantityLoopResult loopResult) {
        if (message.orderSide() == OrderSide.ASK) {
            return loopResult.remainingQuantity();
        }
        if (message.orderType() == OrderType.MARKET) {
            return BigDecimal.ZERO;
        }
        return nonNegativeUnlock(reservedQuoteAmount(message).subtract(loopResult.executedQuoteAmount()));
    }

    private BigDecimal reservedQuoteAmount(CommandMessage.Place message) {
        return roundDownQuote(message.price().multiply(requireQuantity(message)), message);
    }

    private BigDecimal nonNegativeUnlock(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new EngineException("unlockAmount는 음수가 될 수 없습니다.");
        }
        return value;
    }

    private BigDecimal roundDownQuote(BigDecimal value, CommandMessage.Place message) {
        return value.setScale(message.quoteAssetScale(), RoundingMode.DOWN);
    }

    private enum MatchingMode {
        QUANTITY_BASED,
        QUOTE_BASED
    }

    private enum StopReason {
        NO_OPPOSITE,
        NOT_CROSSED,
        TAKER_FILLED,
        NO_EXECUTABLE_SIZE,
        SELF_TRADE_BLOCKED
    }

    private record QuantityLoopResult(
            BigDecimal executedQuantity,
            BigDecimal executedQuoteAmount,
            BigDecimal remainingQuantity,
            List<EngineResult.Fill> fills,
            List<EngineResult.BookDelta> bookDeltas,
            StopReason stopReason
    ) {
        private boolean hasExecution() {
            return executedQuantity.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    private record QuoteLoopResult(
            BigDecimal executedQuantity,
            BigDecimal executedQuoteAmount,
            BigDecimal remainingQuoteAmount,
            List<EngineResult.Fill> fills,
            List<EngineResult.BookDelta> bookDeltas,
            StopReason stopReason
    ) {
        private boolean hasExecution() {
            return executedQuantity.compareTo(BigDecimal.ZERO) > 0;
        }
    }
}
