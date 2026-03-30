package com.project.upbit_clone.trade.application.engine;

import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class EngineResult {

    private EngineResult() {
    }

    public record PlaceResult(
            OrderStatus takerStatus,
            BigDecimal executedQuantity,
            BigDecimal executedQuoteAmount,
            BigDecimal remainingQuantity,
            BigDecimal unlockAmount,
            String cancelReason,
            List<Fill> fills,
            List<BookDelta> bookDeltas
    ) {

        public PlaceResult {
            Objects.requireNonNull(takerStatus, "takerStatus는 null일 수 없습니다.");
            executedQuantity = defaultZeroIfNonNegative(executedQuantity);
            executedQuoteAmount = defaultZeroIfNonNegative(executedQuoteAmount);
            validateNullableNonNegative(remainingQuantity, "엔진 결과 잔량은 0 미만일 수 없습니다.");
            unlockAmount = defaultZeroIfNonNegative(unlockAmount);
            fills = fills == null ? List.of() : List.copyOf(fills);
            bookDeltas = bookDeltas == null ? List.of() : List.copyOf(bookDeltas);
        }

        public static PlaceResult open(BigDecimal remainingQuantity) {
            return new PlaceResult(
                    OrderStatus.OPEN,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    remainingQuantity,
                    BigDecimal.ZERO,
                    null,
                    List.of(),
                    List.of()
            );
        }

        public static PlaceResult canceled(
                BigDecimal remainingQuantity,
                BigDecimal unlockAmount,
                String cancelReason
        ) {
            return new PlaceResult(
                    OrderStatus.CANCELED,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    remainingQuantity,
                    unlockAmount,
                    cancelReason,
                    List.of(),
                    List.of()
            );
        }

        public boolean hasFills() {
            return !fills.isEmpty();
        }
    }

    public record Fill(
            Long makerOrderId,
            BigDecimal price,
            BigDecimal executedQuantity,
            BigDecimal executedQuoteAmount,
            BigDecimal makerRemainingQuantity
    ) {

        public Fill {
            Objects.requireNonNull(makerOrderId, "makerOrderId는 null일 수 없습니다.");
            validatePositive(price, "체결 가격은 0보다 커야 합니다.");
            validatePositive(executedQuantity, "체결 수량은 0보다 커야 합니다.");
            validatePositive(executedQuoteAmount, "체결 금액은 0보다 커야 합니다.");
            makerRemainingQuantity = defaultZeroIfNonNegative(makerRemainingQuantity);
        }
    }

    public record BookDelta(
            InMemoryOrderBook.LevelDelta delta,
            BookDeltaReason reason
    ) {

        public BookDelta {
            Objects.requireNonNull(delta, "delta는 null일 수 없습니다.");
            Objects.requireNonNull(reason, "reason은 null일 수 없습니다.");
        }
    }

    public enum BookDeltaReason {
        RESTING_ORDER_ADDED,
        MATCH_EXECUTED,
        ORDER_CANCELED
    }

    private static BigDecimal defaultZeroIfNonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("엔진 결과 금액은 0 미만일 수 없습니다.");
        }
        return value;
    }

    private static void validateNullableNonNegative(BigDecimal value, String message) {
        if (value == null) {
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void validatePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
