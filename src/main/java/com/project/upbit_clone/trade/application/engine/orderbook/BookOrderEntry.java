package com.project.upbit_clone.trade.application.engine.orderbook;

import com.project.upbit_clone.global.domain.vo.PositiveAmount;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.application.engine.EngineException;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class BookOrderEntry {

    // 오더북 내부 주문 식별값
    private final String orderKey;
    private final OrderSide side;
    private final BigDecimal price;
    private BigDecimal remainingQty;

    // 외부 생성자
    public static BookOrderEntry create(
            String orderKey,
            OrderSide side,
            BigDecimal price,
            BigDecimal remainingQty
    ) {
        validateCreateInput(orderKey, side, price, remainingQty);
        return new BookOrderEntry(orderKey, side, price, remainingQty);
    }

    private BookOrderEntry(
            String orderKey,
            OrderSide side,
            BigDecimal price,
            BigDecimal remainingQty
    ) {
        this.orderKey = orderKey;
        this.side = side;
        this.price = new PositiveAmount(price).value();
        this.remainingQty = new PositiveAmount(remainingQty).value();
    }

    // 생성 입력값 검증.
    private static void validateCreateInput(
            String orderKey,
            OrderSide side,
            BigDecimal price,
            BigDecimal remainingQty
    ) {
        if (orderKey == null || orderKey.isBlank() || side == null || price == null || remainingQty == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_BOOK_INPUT);
        }
    }

    // 체결 수량만큼 잔량을 차감한다. (오더북 내부 전용)
    void decreaseRemainingQty(BigDecimal executedQty) {
        if (executedQty == null || executedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }

        if (remainingQty.compareTo(executedQty) < 0) {
            throw new EngineException("executedQty exceeds remainingQty");
        }
        remainingQty = remainingQty.subtract(executedQty);
    }

    // 잔량이 0이면 완전 체결 상태다.
    public boolean isFilled() {
        return remainingQty.compareTo(BigDecimal.ZERO) == 0;
    }
}
