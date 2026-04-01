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
            CancelReason cancelReason,
            List<Fill> fills,
            List<BookDelta> bookDeltas
    ) {

        public PlaceResult {
            Objects.requireNonNull(takerStatus, "takerStatus는 null일 수 없습니다.");
            executedQuantity = nonNegativeOrZero(executedQuantity);
            executedQuoteAmount = nonNegativeOrZero(executedQuoteAmount);
            remainingQuantity = nullableAndNonNegative(remainingQuantity);
            unlockAmount = nonNegativeOrZero(unlockAmount);
            fills = fills == null ? List.of() : List.copyOf(fills);
            bookDeltas = bookDeltas == null ? List.of() : List.copyOf(bookDeltas);

            validateStatusRules(
                    takerStatus,
                    executedQuantity,
                    executedQuoteAmount,
                    remainingQuantity,
                    cancelReason
            );
            validateFillRules(executedQuantity, executedQuoteAmount, fills);
        }

        public static PlaceResult open(BigDecimal remainingQuantity) {
            return open(remainingQuantity, List.of());
        }

        public static PlaceResult open(
                BigDecimal remainingQuantity,
                List<BookDelta> bookDeltas
        ) {
            return new PlaceResult(
                    OrderStatus.OPEN,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    remainingQuantity,
                    BigDecimal.ZERO,
                    null,
                    List.of(),
                    bookDeltas
            );
        }

        public static PlaceResult filled(
                BigDecimal executedQuantity,
                BigDecimal executedQuoteAmount,
                List<Fill> fills,
                List<BookDelta> bookDeltas
        ) {
            return new PlaceResult(
                    OrderStatus.FILLED,
                    executedQuantity,
                    executedQuoteAmount,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    fills,
                    bookDeltas
            );
        }

        public static PlaceResult canceled(
                BigDecimal remainingQuantity,
                BigDecimal unlockAmount,
                CancelReason cancelReason
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

        public boolean hasExecution() {
            return executedQuantity.compareTo(BigDecimal.ZERO) > 0
                    || executedQuoteAmount.compareTo(BigDecimal.ZERO) > 0;
        }

        private static void validateStatusRules(
                OrderStatus takerStatus,
                BigDecimal executedQuantity,
                BigDecimal executedQuoteAmount,
                BigDecimal remainingQuantity,
                CancelReason cancelReason
        ) {
            switch (takerStatus) {
                case OPEN -> validateOpenRules(remainingQuantity, cancelReason);
                case FILLED -> validateFilledRules(
                        executedQuantity,
                        executedQuoteAmount,
                        remainingQuantity,
                        cancelReason
                );
                case CANCELED -> validateCanceledRules(cancelReason);
            }
        }

        private static void validateOpenRules(BigDecimal remainingQuantity, CancelReason cancelReason) {
            if (remainingQuantity == null || remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new EngineException("OPEN 상태에서는 remainingQuantity가 0보다 커야 합니다.");
            }
            if (cancelReason != null) {
                throw new EngineException("OPEN 상태에서는 cancelReason이 null이어야 합니다.");
            }
        }

        private static void validateFilledRules(
                BigDecimal executedQuantity,
                BigDecimal executedQuoteAmount,
                BigDecimal remainingQuantity,
                CancelReason cancelReason
        ) {
            // 취소 사유 검증
            if (cancelReason != null) {
                throw new EngineException("FILLED 상태에서는 cancelReason이 null이어야 합니다.");
            }
            // 체결 값 검증
            if (executedQuantity.compareTo(BigDecimal.ZERO) <= 0
                    || executedQuoteAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new EngineException("FILLED 상태에서는 체결 값(수량, 금액)이 0보다 커야 합니다.");
            }
            // 잔여 수량 검증
            if (remainingQuantity != null && remainingQuantity.compareTo(BigDecimal.ZERO) != 0) {
                throw new EngineException("FILLED 상태에서는 remainingQuantity가 0 또는 null이어야 합니다.");
            }
        }

        private static void validateCanceledRules(CancelReason cancelReason) {
            if (cancelReason == null) {
                throw new EngineException("CANCELED 상태에서는 cancelReason이 null일 수 없습니다.");
            }
        }

        private static void validateFillRules(
                BigDecimal executedQuantity,
                BigDecimal executedQuoteAmount,
                List<Fill> fills
        ) {
            boolean hasExecution = executedQuantity.compareTo(BigDecimal.ZERO) > 0
                    || executedQuoteAmount.compareTo(BigDecimal.ZERO) > 0;

            if (hasExecution && fills.isEmpty()) {
                throw new EngineException("체결 값이 있으면 fills가 비어 있을 수 없습니다.");
            }
            if (!hasExecution && !fills.isEmpty()) {
                throw new EngineException("체결 값이 없으면 fills는 비어 있어야 합니다.");
            }
            if (fills.isEmpty()) {
                return;
            }

            BigDecimal fillQtySum = fills.stream()
                    .map(Fill::executedQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal fillQuoteSum = fills.stream()
                    .map(Fill::executedQuoteAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (fillQtySum.compareTo(executedQuantity) != 0) {
                throw new EngineException("fills의 quantity 합과 executedQuantity가 일치해야 합니다.");
            }
            if (fillQuoteSum.compareTo(executedQuoteAmount) != 0) {
                throw new EngineException("fills의 quote 합과 executedQuoteAmount가 일치해야 합니다.");
            }
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
            validatePositive(executedQuantity, "executedQuantity는 0보다 커야 합니다.");
            validatePositive(executedQuoteAmount, "executedQuoteAmount는 0보다 커야 합니다.");
            makerRemainingQuantity = nonNegativeOrZero(makerRemainingQuantity);
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

    public enum CancelReason {
        USER_REQUEST,
        IOC_REMAINDER,
        IOC_NOT_MATCHED,
        NO_TRADE_STREAM
    }

    private static BigDecimal nonNegativeOrZero(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new EngineException("결과 값은 0보다 커야 합니다.");
        }
        return value;
    }

    private static BigDecimal nullableAndNonNegative(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            // 현재는 remainingQuantity만 사용.
            throw new EngineException("remainingQuantity는 0보다 커야 합니다.");
        }
        return value;
    }

    private static void validatePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new EngineException(message);
        }
    }
}
