package com.project.upbit_clone.trade.application.engine.orderbook;

import com.project.upbit_clone.global.domain.vo.PositiveAmount;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.application.engine.EngineException;
import com.project.upbit_clone.trade.domain.vo.OrderSide;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public class InMemoryOrderBook {

    // bids 는 내림차순, asks 는 오름차순으로 관리한다.
    private final NavigableMap<BigDecimal, PriceLevel> bidLevels;
    private final NavigableMap<BigDecimal, PriceLevel> askLevels;

    // 주문 취소와 조회를 위한 인덱스
    private final Map<Long, BookOrderEntry> orderIndex;

    public InMemoryOrderBook() {
        this.bidLevels = new TreeMap<>(Comparator.reverseOrder());
        this.askLevels = new TreeMap<>();
        this.orderIndex = new HashMap<>();
    }

    // 주문을 해당 가격 레벨에 적재하고 before/after delta를 반환한다.
    public LevelDelta add(BookOrderEntry entry) {
        validateEntry(entry);
        if (orderIndex.containsKey(entry.getOrderId())) {
            throw new EngineException("중복된 orderId 입니다.: " + entry.getOrderId());
        }

        NavigableMap<BigDecimal, PriceLevel> levels = levels(entry.getSide());

        // entry의 가격이 존재하지 않는다면 생성.
        PriceLevel level = levels.computeIfAbsent(
                entry.getPrice(),
                price -> PriceLevel.create(entry.getSide(), price)
        );

        // 스냅샷을 남긴다.
        PriceLevel.Snapshot before = level.snapshot();
        level.enqueue(entry);
        PriceLevel.Snapshot after = level.snapshot();

        orderIndex.put(entry.getOrderId(), entry);

        return new LevelDelta(entry.getSide(), entry.getPrice(), before, after);
    }

    // 주문을 제거하고 비어 있는 가격 레벨은 함께 정리한다.
    public Optional<LevelDelta> remove(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_BOOK_INPUT);
        }

        BookOrderEntry entry = orderIndex.get(orderId);
        if (entry == null) {
            return Optional.empty();
        }

        NavigableMap<BigDecimal, PriceLevel> levels = levels(entry.getSide());
        PriceLevel level = levels.get(entry.getPrice());
        if (level == null) {
            throw new EngineException("orderId의 price level 을 찾을 수 없습니다.: " + orderId);
        }

        PriceLevel.Snapshot before = level.snapshot();
        boolean removed = level.remove(entry);
        if (!removed) {
            throw new EngineException("price level entry 제거에 실패했습니다.: " + orderId);
        }

        PriceLevel.Snapshot after;
        if (level.isEmpty()) {
            levels.remove(level.getPrice());
            after = PriceLevel.emptySnapshot(entry.getSide(), entry.getPrice());
        } else {
            after = level.snapshot();
        }

        orderIndex.remove(orderId);

        return Optional.of(new LevelDelta(entry.getSide(), entry.getPrice(), before, after));
    }

    // 지정한 가격 레벨의 선두 주문에 부분 체결을 반영하고 before/after delta를 반환한다.
    public LevelDelta applyExecution(OrderSide side, BigDecimal price, BigDecimal executedQty) {
        validateSideAndPrice(side, price);

        NavigableMap<BigDecimal, PriceLevel> levels = levels(side);
        PriceLevel level = levels.get(price);
        if (level == null) {
            throw new EngineException("price level을 찾을 수 없습니다.: " + side + " / " + price);
        }
        BookOrderEntry headEntry = level.peekFirst();
        if (headEntry == null) {
            throw new EngineException("체결할 선두 주문이 없습니다.: " + side + " / " + price);
        }

        PriceLevel.Snapshot before = level.snapshot();
        boolean filled = level.applyExecution(executedQty);

        PriceLevel.Snapshot after;
        if (filled) {
            orderIndex.remove(headEntry.getOrderId());

            if (level.isEmpty()) {
                levels.remove(level.getPrice());
                after = PriceLevel.emptySnapshot(side, price);
            } else {
                after = level.snapshot();
            }
        } else {
            after = level.snapshot();
        }

        return new LevelDelta(side, price, before, after);
    }

    // add 전 엔트리 최소 검증.
    private void validateEntry(BookOrderEntry entry) {
        if (entry == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_BOOK_INPUT);
        }
        validateSideAndPrice(entry.getSide(), entry.getPrice());
    }

    // side / price 공통 검증.
    private void validateSideAndPrice(OrderSide side, BigDecimal price) {
        if (side == null || price == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_BOOK_INPUT);
        }
        new PositiveAmount(price);
    }

    // side 에 맞는 가격 레벨 맵을 반환한다.
    private NavigableMap<BigDecimal, PriceLevel> levels(OrderSide side) {
        return side == OrderSide.BID ? bidLevels : askLevels;
    }

    // 최우선 매수 호가 조회
    public Optional<PriceLevel.Snapshot> getBestBid() {
        return bestSnapshot(bidLevels);
    }

    // 최우선 매도 호가 조회
    public Optional<PriceLevel.Snapshot> getBestAsk() {
        return bestSnapshot(askLevels);
    }

    // side 에 맞는 최우선 가격 레벨 스냅샷을 반환한다.
    private Optional<PriceLevel.Snapshot> bestSnapshot(NavigableMap<BigDecimal, PriceLevel> levels) {
        return Optional.ofNullable(levels.isEmpty() ? null : levels.firstEntry().getValue().snapshot());
    }

    // 특정 가격 레벨 스냅샷 조회
    public Optional<PriceLevel.Snapshot> getLevelSnapshot(OrderSide side, BigDecimal price) {
        validateSideAndPrice(side, price);
        return Optional.ofNullable(levels(side).get(price))
                .map(PriceLevel::snapshot);
    }

    // orderId 기준 엔트리 조회
    public Optional<BookOrderEntry> findOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_BOOK_INPUT);
        }
        return Optional.ofNullable(orderIndex.get(orderId));
    }

    // 현재 bid 가격 레벨 개수
    public int getBidLevelCount() {
        return bidLevels.size();
    }

    // 현재 ask 가격 레벨 개수
    public int getAskLevelCount() {
        return askLevels.size();
    }

    public record LevelDelta(
            OrderSide side,
            BigDecimal price,
            PriceLevel.Snapshot before,
            PriceLevel.Snapshot after
    ) {
    }
}
