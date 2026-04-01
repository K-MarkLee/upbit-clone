package com.project.upbit_clone.trade.application.engine.orderbook;

import com.project.upbit_clone.trade.application.engine.EngineException;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryOrderBook 테스트")
class InMemoryOrderBookTest {
    @Test
    @DisplayName("Happy : bid 주문 1건을 추가하면 best bid와 레벨 상태가 갱신된다.")
    void add_single_bid_order() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        BookOrderEntry entry = BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("1.25")
        );

        InMemoryOrderBook.LevelDelta delta = orderBook.add(entry);

        assertThat(delta.before().totalQty()).isEqualByComparingTo("0");
        assertThat(delta.before().orderCount()).isZero();
        assertThat(delta.after().totalQty()).isEqualByComparingTo("1.25");
        assertThat(delta.after().orderCount()).isEqualTo(1);
        assertThat(orderBook.getBestBid()).isPresent();
        assertThat(orderBook.getBestBid().orElseThrow().price()).isEqualByComparingTo("50000");
        assertThat(orderBook.getBestAsk()).isEmpty();
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000"))).isPresent();
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000")).orElseThrow().totalQty())
                .isEqualByComparingTo("1.25");
    }

    @Test
    @DisplayName("Happy : previewAdd는 delta만 계산하고 오더북 상태는 변경하지 않는다.")
    void preview_add_returns_delta_without_mutating_order_book() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        BookOrderEntry entry = BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("1.25")
        );

        InMemoryOrderBook.LevelDelta delta = orderBook.previewAdd(entry);

        assertThat(delta.before().totalQty()).isEqualByComparingTo("0");
        assertThat(delta.before().orderCount()).isZero();
        assertThat(delta.after().totalQty()).isEqualByComparingTo("1.25");
        assertThat(delta.after().orderCount()).isEqualTo(1);
        assertThat(orderBook.getBestBid()).isEmpty();
        assertThat(orderBook.findOrder(101L)).isEmpty();
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000"))).isEmpty();
    }

    @Test
    @DisplayName("Happy : 같은 가격 주문은 FIFO 순서로 누적된다.")
    void keep_fifo_order_at_same_price_level() {
        PriceLevel level = PriceLevel.create(OrderSide.BID, new BigDecimal("50000"));
        BookOrderEntry first = BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("1.0")
        );
        BookOrderEntry second = BookOrderEntry.create(
                102L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("2.0")
        );

        level.enqueue(first);
        level.enqueue(second);

        assertThat(level.peekFirst()).isEqualTo(first);
        assertThat(level.peekLast()).isEqualTo(second);
        assertThat(level.getOrderCount()).isEqualTo(2);
        assertThat(level.getTotalQty()).isEqualByComparingTo("3.0");
    }

    @Test
    @DisplayName("Happy : bid는 높은 가격, ask는 낮은 가격이 최우선 호가가 된다.")
    void track_best_quotes_by_side() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();

        orderBook.add(BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("1.0")
        ));
        orderBook.add(BookOrderEntry.create(
                102L,
                OrderSide.BID,
                new BigDecimal("51000"),
                new BigDecimal("1.0")
        ));
        orderBook.add(BookOrderEntry.create(
                201L,
                OrderSide.ASK,
                new BigDecimal("52000"),
                new BigDecimal("1.0")
        ));
        orderBook.add(BookOrderEntry.create(
                202L,
                OrderSide.ASK,
                new BigDecimal("51500"),
                new BigDecimal("1.0")
        ));

        assertThat(orderBook.getBestBid()).isPresent();
        assertThat(orderBook.getBestBid().orElseThrow().price()).isEqualByComparingTo("51000");
        assertThat(orderBook.getBestAsk()).isPresent();
        assertThat(orderBook.getBestAsk().orElseThrow().price()).isEqualByComparingTo("51500");
    }

    @Test
    @DisplayName("Happy : best bid/ask head는 최우선 가격 레벨의 선두 주문을 반환한다.")
    void track_best_head_order_by_side() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        BookOrderEntry bestBidHead = BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("51000"),
                new BigDecimal("1.0")
        );
        BookOrderEntry sameBestBidSecond = BookOrderEntry.create(
                102L,
                OrderSide.BID,
                new BigDecimal("51000"),
                new BigDecimal("2.0")
        );
        BookOrderEntry lowerBid = BookOrderEntry.create(
                103L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("3.0")
        );
        BookOrderEntry bestAskHead = BookOrderEntry.create(
                201L,
                OrderSide.ASK,
                new BigDecimal("51500"),
                new BigDecimal("1.0")
        );
        BookOrderEntry sameBestAskSecond = BookOrderEntry.create(
                202L,
                OrderSide.ASK,
                new BigDecimal("51500"),
                new BigDecimal("2.0")
        );
        BookOrderEntry higherAsk = BookOrderEntry.create(
                203L,
                OrderSide.ASK,
                new BigDecimal("52000"),
                new BigDecimal("3.0")
        );

        orderBook.add(lowerBid);
        orderBook.add(bestBidHead);
        orderBook.add(sameBestBidSecond);
        orderBook.add(higherAsk);
        orderBook.add(bestAskHead);
        orderBook.add(sameBestAskSecond);

        assertThat(orderBook.getBestBidHead()).contains(bestBidHead);
        assertThat(orderBook.getBestAskHead()).contains(bestAskHead);
    }

    @Test
    @DisplayName("Happy : 빈 오더북에서는 best bid/ask head가 비어 있다.")
    void best_head_is_empty_when_order_book_is_empty() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();

        assertThat(orderBook.getBestBidHead()).isEmpty();
        assertThat(orderBook.getBestAskHead()).isEmpty();
    }

    @Test
    @DisplayName("Happy : 마지막 주문을 제거하면 가격 레벨이 비워지고 북에서도 제거된다.")
    void remove_last_order_and_delete_price_level() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        orderBook.add(BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("1.0")
        ));

        InMemoryOrderBook.LevelDelta delta = orderBook.remove(101L).orElseThrow();

        assertThat(delta.before().totalQty()).isEqualByComparingTo("1.0");
        assertThat(delta.before().orderCount()).isEqualTo(1);
        assertThat(delta.after().totalQty()).isEqualByComparingTo("0");
        assertThat(delta.after().orderCount()).isZero();
        assertThat(orderBook.findOrder(101L)).isEmpty();
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000"))).isEmpty();
        assertThat(orderBook.getBestBid()).isEmpty();
    }

    @Test
    @DisplayName("Happy : 부분 체결 후 레벨 총수량과 스냅샷이 함께 갱신된다.")
    void apply_partial_execution_and_update_level_snapshot() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        orderBook.add(BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("10")
        ));
        orderBook.add(BookOrderEntry.create(
                102L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("5")
        ));

        InMemoryOrderBook.LevelDelta delta = orderBook.applyExecution(
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("4")
        );

        assertThat(delta.before().totalQty()).isEqualByComparingTo("15");
        assertThat(delta.before().orderCount()).isEqualTo(2);
        assertThat(delta.after().totalQty()).isEqualByComparingTo("11");
        assertThat(delta.after().orderCount()).isEqualTo(2);
        assertThat(orderBook.findOrder(101L)).isPresent();
        assertThat(orderBook.findOrder(101L).orElseThrow().getRemainingQty()).isEqualByComparingTo("6");
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000"))).isPresent();
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000")).orElseThrow().totalQty())
                .isEqualByComparingTo("11");
    }

    @Test
    @DisplayName("Happy : 부분 체결된 주문을 제거하면 남은 레벨 총수량이 정확히 유지된다.")
    void remove_partially_filled_order_and_keep_correct_total_qty() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        orderBook.add(BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("10")
        ));
        orderBook.add(BookOrderEntry.create(
                102L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("5")
        ));

        orderBook.applyExecution(
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("4")
        );
        InMemoryOrderBook.LevelDelta delta = orderBook.remove(101L).orElseThrow();

        assertThat(delta.before().totalQty()).isEqualByComparingTo("11");
        assertThat(delta.before().orderCount()).isEqualTo(2);
        assertThat(delta.after().totalQty()).isEqualByComparingTo("5");
        assertThat(delta.after().orderCount()).isEqualTo(1);
        assertThat(orderBook.findOrder(101L)).isEmpty();
        assertThat(orderBook.findOrder(102L)).isPresent();
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000"))).isPresent();
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000")).orElseThrow().totalQty())
                .isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("Happy : 마지막 주문이 전량 체결되면 레벨과 인덱스에서 함께 제거된다.")
    void apply_full_execution_and_remove_last_order_from_book() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        orderBook.add(BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("10")
        ));

        InMemoryOrderBook.LevelDelta delta = orderBook.applyExecution(
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("10")
        );

        assertThat(delta.before().totalQty()).isEqualByComparingTo("10");
        assertThat(delta.before().orderCount()).isEqualTo(1);
        assertThat(delta.after().totalQty()).isEqualByComparingTo("0");
        assertThat(delta.after().orderCount()).isZero();
        assertThat(orderBook.findOrder(101L)).isEmpty();
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000"))).isEmpty();
        assertThat(orderBook.getBestBid()).isEmpty();
    }

    @Test
    @DisplayName("Happy : 같은 가격 레벨 체결은 항상 선두 주문부터 반영된다.")
    void apply_execution_to_head_order_only() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        orderBook.add(BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("10")
        ));
        orderBook.add(BookOrderEntry.create(
                102L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("5")
        ));

        InMemoryOrderBook.LevelDelta delta = orderBook.applyExecution(
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("10")
        );

        assertThat(delta.before().totalQty()).isEqualByComparingTo("15");
        assertThat(delta.after().totalQty()).isEqualByComparingTo("5");
        assertThat(orderBook.findOrder(101L)).isEmpty();
        assertThat(orderBook.findOrder(102L)).isPresent();
        assertThat(orderBook.findOrder(102L).orElseThrow().getRemainingQty()).isEqualByComparingTo("5");
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000"))).isPresent();
        assertThat(orderBook.getLevelSnapshot(OrderSide.BID, new BigDecimal("50000")).orElseThrow().orderCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Negative : 전량 체결된 entry는 다시 오더북에 추가할 수 없다.")
    void reject_readding_filled_entry() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        BookOrderEntry entry = BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("10")
        );

        orderBook.add(entry);
        orderBook.applyExecution(
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("10")
        );

        assertThatThrownBy(() -> orderBook.add(entry))
                .isInstanceOf(EngineException.class)
                .hasMessageContaining("101");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0"})
    @DisplayName("Negative : 0 이하 체결 수량은 허용하지 않는다.")
    void reject_non_positive_execution_quantity(String executedQty) {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        orderBook.add(BookOrderEntry.create(
                101L,
                OrderSide.BID,
                new BigDecimal("50000"),
                new BigDecimal("10")
        ));

        BigDecimal price = new BigDecimal("50000");
        BigDecimal qty = new BigDecimal(executedQty);

        assertThatThrownBy(() -> orderBook.applyExecution(
                OrderSide.BID, price, qty))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
