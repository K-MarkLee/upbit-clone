package com.project.upbit_clone.trade.application.engine.orderbook;

import com.project.upbit_clone.trade.application.engine.EngineException;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class BookOrderEntry {

    private final String orderKey;
    private final Long userId;
    private final OrderSide side;
    private final BigDecimal price;
    private BigDecimal remainingQty;

    public static BookOrderEntry create(
            String orderKey,
            Long userId,
            OrderSide side,
            BigDecimal price,
            BigDecimal remainingQty
    ) {
        validateCreateInput(orderKey, userId, side, price, remainingQty);
        return new BookOrderEntry(orderKey, userId, side, price, remainingQty);
    }

    private BookOrderEntry(
            String orderKey,
            Long userId,
            OrderSide side,
            BigDecimal price,
            BigDecimal remainingQty
    ) {
        this.orderKey = orderKey;
        this.userId = userId;
        this.side = side;
        this.price = price;
        this.remainingQty = remainingQty;
    }

    private static void validateCreateInput(
            String orderKey,
            Long userId,
            OrderSide side,
            BigDecimal price,
            BigDecimal remainingQty
    ) {
        if (orderKey == null || orderKey.isBlank() || userId == null || side == null || price == null || remainingQty == null) {
            throw new EngineException("order entry 필수값이 누락되어 있습니다.");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new EngineException("price는 0보다 커야 합니다.");
        }
        if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new EngineException("remainingQty는 0보다 커야 합니다.");
        }
    }

    void decreaseRemainingQty(BigDecimal executedQty) {
        if (executedQty == null || executedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new EngineException("executedQty는 0보다 커야 합니다.");
        }
        if (remainingQty.compareTo(executedQty) < 0) {
            throw new EngineException("executedQty exceeds remainingQty");
        }
        remainingQty = remainingQty.subtract(executedQty);
    }

    public boolean isFilled() {
        return remainingQty.compareTo(BigDecimal.ZERO) == 0;
    }
}
