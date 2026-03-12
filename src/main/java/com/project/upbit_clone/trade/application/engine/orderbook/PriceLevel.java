package com.project.upbit_clone.trade.application.engine.orderbook;

import com.project.upbit_clone.global.domain.vo.PositiveAmount;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.application.engine.EngineException;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Getter
public class PriceLevel {

    // 동일 가격대 주문 큐
    private final OrderSide side;
    private final BigDecimal price;
    private final Deque<BookOrderEntry> entries;
    private BigDecimal totalQty;
    private int orderCount;

    private PriceLevel(OrderSide side, BigDecimal price) {
        this.side = side;
        this.price = new PositiveAmount(price).value();
        this.entries = new ArrayDeque<>();
        this.totalQty = BigDecimal.ZERO;
        this.orderCount = 0;
    }

    // 가격 레벨 생성
    public static PriceLevel create(OrderSide side, BigDecimal price) {
        validateCreateInput(side, price);
        return new PriceLevel(side, price);
    }

    // 동일가 주문을 FIFO 순서로 적재한다.
    public void enqueue(BookOrderEntry entry) {
        validateEntry(entry);
        entries.addLast(entry);
        totalQty = totalQty.add(entry.getRemainingQty());
        orderCount++;
    }

    // 주문을 제거하고 레벨 집계를 갱신한다.
    public boolean remove(BookOrderEntry entry) {
        if (entry == null) {
            return false;
        }
        boolean removed = entries.remove(entry);
        if (!removed) {
            return false;
        }

        totalQty = totalQty.subtract(entry.getRemainingQty());
        orderCount--;
        return true;
    }

    // 선두 주문에 부분 체결을 적용하고 레벨 집계를 함께 갱신한다.
    public boolean applyExecution(BigDecimal executedQty) {
        BigDecimal value = new PositiveAmount(executedQty).value();
        BookOrderEntry entry = entries.peekFirst();
        if (entry == null) {
            throw new EngineException("체결할 선두 주문이 없습니다.");
        }

        entry.decreaseRemainingQty(value);
        totalQty = totalQty.subtract(value);

        if (entry.isFilled()) {
            BookOrderEntry removed = entries.pollFirst();
            if (removed == null) {
                throw new EngineException("entry를 제거하는데 실패했습니다.");
            }
            orderCount--;
            return true;
        }

        return false;
    }

    // 레벨이 제거된 이후 상태 표현용 빈 스냅샷
    public static Snapshot emptySnapshot(OrderSide side, BigDecimal price) {
        validateCreateInput(side, price);
        return new Snapshot(side, price, BigDecimal.ZERO, 0);
    }

    // 생성 입력값 검증.
    private static void validateCreateInput(OrderSide side, BigDecimal price) {
        if (side == null || price == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_BOOK_INPUT);
        }
    }

    // 엔트리와 레벨의 side/price 일치 여부 검증.
    private void validateEntry(BookOrderEntry entry) {
        if (entry == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_BOOK_INPUT);
        }
        if (entry.getSide() != side) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_BOOK_INPUT);
        }
        if (entry.getPrice().compareTo(price) != 0) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_BOOK_INPUT);
        }
    }

    // 현재 레벨의 최우선 주문을 조회한다.
    public BookOrderEntry peekFirst() {
        return entries.peekFirst();
    }

    // 현재 레벨의 마지막 주문을 조회한다.
    public BookOrderEntry peekLast() {
        return entries.peekLast();
    }

    // 레벨이 비었는지 확인한다.
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    // 오더북 delta 계산용 레벨 상태를 생성한다.
    public Snapshot snapshot() {
        return new Snapshot(side, price, totalQty, orderCount);
    }

    // 테스트 및 디버깅용 읽기 전용 뷰
    public List<BookOrderEntry> entriesView() {
        return List.copyOf(new ArrayList<>(entries));
    }

    public record Snapshot(
            OrderSide side,
            BigDecimal price,
            BigDecimal totalQty,
            int orderCount
    ) {
    }
}
