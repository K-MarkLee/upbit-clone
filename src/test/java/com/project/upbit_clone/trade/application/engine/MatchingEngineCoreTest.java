package com.project.upbit_clone.trade.application.engine;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.orderbook.BookOrderEntry;
import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.application.engine.orderbook.PriceLevel;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MatchingEngineCore 단위 테스트")
class MatchingEngineCoreTest {

    private MatchingEngineCore matchingEngineCore;
    private Long userId;
    private Long marketId;
    private String marketCode;
    private int baseAssetScale;
    private int quoteAssetScale;
    private long nextCommandLogId;

    @BeforeEach
    void setUp() {
        matchingEngineCore = new MatchingEngineCore();
        userId = 10L;
        marketId = 100L;
        marketCode = "KRW-BTC";
        baseAssetScale = 8;
        quoteAssetScale = 8;
        nextCommandLogId = 1L;
    }

    @Test
    @DisplayName("Happy : LIMIT-BID 주문은 교차 ask가 있으면 거래에 성공한다.")
    void place_limit_bid_with_crossed_ask_returns_filled() {
        // given
        InMemoryOrderBook orderBook = orderBook(maker(101L, OrderSide.ASK, "900", "2"));
        CommandMessage.Place message = limitBid("1000", "2");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertFilledResult(result, "2", "1800", "200");
        assertSingleFill(result, orderKey(101L), "900", "2", "1800");
        assertSingleMatchDelta(result, OrderSide.ASK, "900", "2");
        assertThat(orderBook.findOrder(orderKey(101L))).isEmpty();
    }

    @Test
    @DisplayName("Happy : LIMIT-ASK 주문은 교차 bid가 있으면 거래에 성공한다.")
    void place_limit_ask_with_crossed_bid_returns_filled() {
        // given
        InMemoryOrderBook orderBook = orderBook(maker(102L, OrderSide.BID, "1100", "2"));
        CommandMessage.Place message = limitAsk("1000", "2");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertFilledResult(result, "2", "2200", "0");
        assertSingleFill(result, orderKey(102L), "1100", "2", "2200");
        assertSingleMatchDelta(result, OrderSide.BID, "1100", "2");
        assertThat(orderBook.findOrder(orderKey(102L))).isEmpty();
    }

    @Test
    @DisplayName("Happy : MARKET-BID 주문은 체결 가능한 금액이 있으면 거래에 성공한다.")
    void place_market_bid_with_executable_quote_returns_filled() {
        // given
        InMemoryOrderBook orderBook = orderBook(maker(103L, OrderSide.ASK, "7", "2"));
        CommandMessage.Place message = marketBid("14");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertFilledResult(result, "2", "14", "0");
        assertSingleFill(result, orderKey(103L), "7", "2", "14");
        assertSingleMatchDelta(result, OrderSide.ASK, "7", "2");
        assertThat(orderBook.findOrder(orderKey(103L))).isEmpty();
    }

    @Test
    @DisplayName("Happy : MARKET-ASK 주문은 교차 bid가 있으면 거래에 성공한다.")
    void place_market_ask_with_crossed_bid_returns_filled() {
        // given
        InMemoryOrderBook orderBook = orderBook(maker(104L, OrderSide.BID, "1000", "2"));
        CommandMessage.Place message = marketAsk("2");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertFilledResult(result, "2", "2000", "0");
        assertSingleFill(result, orderKey(104L), "1000", "2", "2000");
        assertSingleMatchDelta(result, OrderSide.BID, "1000", "2");
        assertThat(orderBook.findOrder(orderKey(104L))).isEmpty();
    }

    @Test
    @DisplayName("Happy : quantity 기반 체결 금액은 quoteAssetScale 기준으로 내림 처리된다.")
    void place_quantity_based_order_rounds_executed_quote_amount_down_by_quote_asset_scale() {
        // given
        InMemoryOrderBook orderBook = orderBook(maker(301L, OrderSide.ASK, "3.333", "3"));
        CommandMessage.Place message = limitBid("4", "3", 2);

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertFilledResult(result, "3", "9.99", "2.01");
        assertSingleFill(result, orderKey(301L), "3.333", "3", "9.99");
        assertSingleMatchDelta(result, OrderSide.ASK, "3.333", "3");
        assertThat(result.executedQuoteAmount().scale()).isEqualTo(2);
        assertThat(result.unlockAmount().scale()).isEqualTo(2);
        assertThat(result.fills().getFirst().executedQuoteAmount().scale()).isEqualTo(2);
        assertThat(orderBook.findOrder(orderKey(301L))).isEmpty();
    }

    @Test
    @DisplayName("Happy : quote 기반 체결 금액은 quoteAssetScale 기준으로 내림 처리된다.")
    void place_quote_based_order_rounds_executed_quote_amount_down_by_quote_asset_scale() {
        // given
        InMemoryOrderBook orderBook = orderBook(maker(302L, OrderSide.ASK, "1.234", "10"));
        CommandMessage.Place message = marketBid("20", baseAssetScale, 2);

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(result.executedQuantity()).isEqualByComparingTo("10");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("12.34");
        assertThat(result.executedQuoteAmount().scale()).isEqualTo(2);
        assertThat(result.remainingQuantity()).isNull();
        assertThat(result.unlockAmount()).isEqualByComparingTo("7.66");
        assertThat(result.unlockAmount().scale()).isEqualTo(2);
        assertThat(result.cancelReason()).isEqualTo(EngineResult.CancelReason.IOC_REMAINDER);
        assertThat(result.fills()).hasSize(1);
        assertThat(result.bookDeltas()).hasSize(1);

        assertSingleFill(result, orderKey(302L), "1.234", "10", "12.34");
        assertSingleMatchDelta(result, OrderSide.ASK, "1.234", "10");
        assertThat(result.fills().getFirst().executedQuoteAmount().scale()).isEqualTo(2);
        assertThat(orderBook.findOrder(orderKey(302L))).isEmpty();
    }

    @Test
    @DisplayName("Happy : LIMIT-BID 부분 체결 unlockAmount는 quoteAssetScale 기준 reserve로 계산된다.")
    void place_limit_bid_partial_execution_rounds_open_unlock_amount_by_quote_asset_scale() {
        // given
        InMemoryOrderBook orderBook = orderBook(maker(303L, OrderSide.ASK, "9.999", "1"));
        CommandMessage.Place message = limitBid("10.009", "2", 2);

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(result.executedQuantity()).isEqualByComparingTo("1");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("9.99");
        assertThat(result.executedQuoteAmount().scale()).isEqualTo(2);
        assertThat(result.remainingQuantity()).isEqualByComparingTo("1");
        assertThat(result.unlockAmount()).isEqualByComparingTo("0.02");
        assertThat(result.unlockAmount().scale()).isEqualTo(2);
        assertThat(result.cancelReason()).isNull();
        assertThat(result.fills()).hasSize(1);
        assertThat(result.bookDeltas()).hasSize(2);

        assertSingleFill(result, orderKey(303L), "9.999", "1", "9.99");
        assertMatchDeltaAt(result, 0, OrderSide.ASK, "9.999", "1", 1, "0", 0);
        assertRestingDeltaAt(result, 1, OrderSide.BID, "10.009", "0", 0, "1", 1);
        assertRestingOrder(orderBook, message.orderKey(), OrderSide.BID, "10.009", "1");
        assertThat(result.fills().getFirst().executedQuoteAmount().scale()).isEqualTo(2);
        assertThat(orderBook.findOrder(orderKey(303L))).isEmpty();
    }

    @Test
    @DisplayName("Happy : LIMIT-BID 주문은 여러 ask maker를 순차 체결할 수 있다.")
    void place_limit_bid_with_multiple_crossed_asks_returns_filled() {
        // given
        InMemoryOrderBook orderBook = orderBook(
                maker(105L, OrderSide.ASK, "900", "2"),
                maker(106L, OrderSide.ASK, "950", "3")
        );
        CommandMessage.Place message = limitBid("1000", "5");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.executedQuantity()).isEqualByComparingTo("5");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("4650");
        assertThat(result.remainingQuantity()).isEqualByComparingTo("0");
        assertThat(result.unlockAmount()).isEqualByComparingTo("350");
        assertThat(result.cancelReason()).isNull();
        assertThat(result.fills()).hasSize(2);
        assertThat(result.bookDeltas()).hasSize(2);

        assertFillAt(result, 0, orderKey(105L), "900", "2", "1800", "0");
        assertFillAt(result, 1, orderKey(106L), "950", "3", "2850", "0");
        assertMatchDeltaAt(result, 0, OrderSide.ASK, "900", "2", 1, "0", 0);
        assertMatchDeltaAt(result, 1, OrderSide.ASK, "950", "3", 1, "0", 0);
        assertThat(orderBook.findOrder(orderKey(105L))).isEmpty();
        assertThat(orderBook.findOrder(orderKey(106L))).isEmpty();
    }

    @Test
    @DisplayName("Negative : LIMIT-BID 주문은 교차 ask가 없으면 resting 된다.")
    void place_limit_bid_without_crossed_ask_returns_open() {
        // given
        InMemoryOrderBook orderBook = orderBook(maker(201L, OrderSide.ASK, "1100", "2"));
        CommandMessage.Place message = limitBid("1000", "2");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertOpenWithoutExecution(result, "2");
        assertSingleRestingDelta(result, OrderSide.BID, "1000", "2");
        assertRestingOrder(orderBook, message.orderKey(), OrderSide.BID, "1000", "2");
        assertThat(requiredLevelSnapshot(orderBook, OrderSide.ASK, decimal("1100")).totalQty())
                .isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("Negative : LIMIT-BID 주문은 최우선 교차 ask가 자기 주문이면 CN 정책으로 취소된다.")
    void place_limit_bid_with_self_crossed_ask_returns_canceled() {
        // given
        InMemoryOrderBook orderBook = orderBook(
                BookOrderEntry.create(orderKey(901L), userId, OrderSide.ASK, decimal("900"), decimal("2"))
        );
        CommandMessage.Place message = limitBid("1000", "2");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertCanceledWithoutExecution(result, "2", "2000", EngineResult.CancelReason.SELF_TRADE_PREVENTED);
        assertThat(orderBook.findOrder(orderKey(901L))).isPresent();
        assertThat(orderBook.findOrder(message.orderKey())).isEmpty();
        assertThat(requiredLevelSnapshot(orderBook, OrderSide.ASK, decimal("900")).totalQty())
                .isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("Negative : LIMIT-ASK 주문은 교차 bid가 없으면 resting 된다.")
    void place_limit_ask_without_crossed_bid_returns_open() {
        // given
        InMemoryOrderBook orderBook = orderBook(maker(202L, OrderSide.BID, "900", "2"));
        CommandMessage.Place message = limitAsk("1000", "2");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertOpenWithoutExecution(result, "2");
        assertSingleRestingDelta(result, OrderSide.ASK, "1000", "2");
        assertRestingOrder(orderBook, message.orderKey(), OrderSide.ASK, "1000", "2");
        assertThat(requiredLevelSnapshot(orderBook, OrderSide.BID, decimal("900")).totalQty())
                .isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("Negative : LIMIT-BID 주문은 일부 체결 후 잔량을 오더북에 resting 한다.")
    void place_limit_bid_with_partial_execution_rests_remaining_quantity() {
        // given
        InMemoryOrderBook orderBook = orderBook(
                maker(205L, OrderSide.ASK, "900", "2"),
                maker(206L, OrderSide.ASK, "950", "1")
        );
        CommandMessage.Place message = limitBid("1000", "5");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(result.executedQuantity()).isEqualByComparingTo("3");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("2750");
        assertThat(result.remainingQuantity()).isEqualByComparingTo("2");
        assertThat(result.unlockAmount()).isEqualByComparingTo("250");
        assertThat(result.cancelReason()).isNull();
        assertThat(result.fills()).hasSize(2);
        assertThat(result.bookDeltas()).hasSize(3);

        assertFillAt(result, 0, orderKey(205L), "900", "2", "1800", "0");
        assertFillAt(result, 1, orderKey(206L), "950", "1", "950", "0");
        assertMatchDeltaAt(result, 0, OrderSide.ASK, "900", "2", 1, "0", 0);
        assertMatchDeltaAt(result, 1, OrderSide.ASK, "950", "1", 1, "0", 0);
        assertRestingDeltaAt(result, 2, OrderSide.BID, "1000", "0", 0, "2", 1);
        assertRestingOrder(orderBook, message.orderKey(), OrderSide.BID, "1000", "2");
        assertThat(orderBook.findOrder(orderKey(205L))).isEmpty();
        assertThat(orderBook.findOrder(orderKey(206L))).isEmpty();
    }

    @Test
    @DisplayName("Negative : MARKET-BID 주문은 최소 체결 수량을 만들 수 없으면 실패한다.")
    void place_market_bid_without_executable_quote_returns_canceled() {
        // given
        InMemoryOrderBook orderBook = orderBook(maker(203L, OrderSide.ASK, "7", "5"));
        CommandMessage.Place message = marketBid("3", 0, 1);

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertCanceledWithoutExecution(result, null, "3", EngineResult.CancelReason.IOC_NOT_MATCHED);
        assertThat(orderBook.findOrder(orderKey(203L))).isPresent();
        assertThat(requiredLevelSnapshot(orderBook, OrderSide.ASK, decimal("7")).totalQty())
                .isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("Negative : MARKET-BID 주문은 여러 ask maker를 체결하고 남은 금액을 IOC remainder로 취소한다.")
    void place_market_bid_with_multiple_asks_returns_canceled_with_ioc_remainder() {
        // given
        InMemoryOrderBook orderBook = orderBook(
                maker(207L, OrderSide.ASK, "7", "1"),
                maker(208L, OrderSide.ASK, "8", "1")
        );
        CommandMessage.Place message = marketBid("20");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(result.executedQuantity()).isEqualByComparingTo("2");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("15");
        assertThat(result.remainingQuantity()).isNull();
        assertThat(result.unlockAmount()).isEqualByComparingTo("5");
        assertThat(result.cancelReason()).isEqualTo(EngineResult.CancelReason.IOC_REMAINDER);
        assertThat(result.fills()).hasSize(2);
        assertThat(result.bookDeltas()).hasSize(2);

        assertFillAt(result, 0, orderKey(207L), "7", "1", "7", "0");
        assertFillAt(result, 1, orderKey(208L), "8", "1", "8", "0");
        assertMatchDeltaAt(result, 0, OrderSide.ASK, "7", "1", 1, "0", 0);
        assertMatchDeltaAt(result, 1, OrderSide.ASK, "8", "1", 1, "0", 0);
        assertThat(orderBook.findOrder(orderKey(207L))).isEmpty();
        assertThat(orderBook.findOrder(orderKey(208L))).isEmpty();
    }

    @Test
    @DisplayName("Negative : MARKET-BID 주문은 최우선 ask가 자기 주문이면 CN 정책으로 취소된다.")
    void place_market_bid_with_self_best_ask_returns_canceled() {
        // given
        InMemoryOrderBook orderBook = orderBook(
                BookOrderEntry.create(orderKey(902L), userId, OrderSide.ASK, decimal("7"), decimal("2"))
        );
        CommandMessage.Place message = marketBid("14");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertCanceledWithoutExecution(result, null, "14", EngineResult.CancelReason.SELF_TRADE_PREVENTED);
        assertThat(orderBook.findOrder(orderKey(902L))).isPresent();
        assertThat(requiredLevelSnapshot(orderBook, OrderSide.ASK, decimal("7")).totalQty())
                .isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("Negative : MARKET-ASK 주문은 반대편 bid가 없으면 실패한다.")
    void place_market_ask_without_bid_book_returns_canceled() {
        // given
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        CommandMessage.Place message = marketAsk("2");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertCanceledWithoutExecution(result, "2", "2", EngineResult.CancelReason.NO_TRADE_STREAM);
        assertThat(orderBook.getBestBidHead()).isEmpty();
    }

    @Test
    @DisplayName("Negative : MARKET-ASK 주문은 여러 bid maker를 체결하고 남은 수량을 IOC remainder로 취소한다.")
    void place_market_ask_with_multiple_bids_returns_canceled_with_ioc_remainder() {
        // given
        InMemoryOrderBook orderBook = orderBook(
                maker(209L, OrderSide.BID, "1100", "2"),
                maker(210L, OrderSide.BID, "1000", "1")
        );
        CommandMessage.Place message = marketAsk("4");

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(result.executedQuantity()).isEqualByComparingTo("3");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("3200");
        assertThat(result.remainingQuantity()).isEqualByComparingTo("1");
        assertThat(result.unlockAmount()).isEqualByComparingTo("1");
        assertThat(result.cancelReason()).isEqualTo(EngineResult.CancelReason.IOC_REMAINDER);
        assertThat(result.fills()).hasSize(2);
        assertThat(result.bookDeltas()).hasSize(2);

        assertFillAt(result, 0, orderKey(209L), "1100", "2", "2200", "0");
        assertFillAt(result, 1, orderKey(210L), "1000", "1", "1000", "0");
        assertMatchDeltaAt(result, 0, OrderSide.BID, "1100", "2", 1, "0", 0);
        assertMatchDeltaAt(result, 1, OrderSide.BID, "1000", "1", 1, "0", 0);
        assertThat(orderBook.findOrder(orderKey(209L))).isEmpty();
        assertThat(orderBook.findOrder(orderKey(210L))).isEmpty();
    }

    @Test
    @DisplayName("Negative : message가 null이면 NullPointerException을 반환한다.")
    void place_with_null_message() {
        // given
        InMemoryOrderBook orderBook = new InMemoryOrderBook();

        // when & then
        assertThatThrownBy(() -> matchingEngineCore.place(null, orderBook))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("message는 null일 수 없습니다.");
    }

    @Test
    @DisplayName("Negative : orderBook이 null이면 NullPointerException을 반환한다.")
    void place_with_null_order_book() {
        // given
        CommandMessage.Place message = limitBid("1000", "2");

        // when & then
        assertThatThrownBy(() -> matchingEngineCore.place(message, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("orderBook은 null일 수 없습니다.");
    }

    private void assertFilledResult(
            EngineResult.PlaceResult result,
            String executedQuantity,
            String executedQuoteAmount,
            String unlockAmount
    ) {
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.executedQuantity()).isEqualByComparingTo(executedQuantity);
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo(executedQuoteAmount);
        assertThat(result.remainingQuantity()).isEqualByComparingTo("0");
        assertThat(result.unlockAmount()).isEqualByComparingTo(unlockAmount);
        assertThat(result.cancelReason()).isNull();
        assertThat(result.fills()).hasSize(1);
        assertThat(result.bookDeltas()).hasSize(1);
    }

    private void assertSingleFill(
            EngineResult.PlaceResult result,
            String makerOrderKey,
            String price,
            String executedQuantity,
            String executedQuoteAmount
    ) {
        EngineResult.Fill fill = result.fills().getFirst();

        assertThat(fill.makerOrderKey()).isEqualTo(makerOrderKey);
        assertThat(fill.price()).isEqualByComparingTo(price);
        assertThat(fill.executedQuantity()).isEqualByComparingTo(executedQuantity);
        assertThat(fill.executedQuoteAmount()).isEqualByComparingTo(executedQuoteAmount);
        assertThat(fill.makerRemainingQuantity()).isEqualByComparingTo("0");
    }

    private void assertFillAt(
            EngineResult.PlaceResult result,
            int index,
            String makerOrderKey,
            String price,
            String executedQuantity,
            String executedQuoteAmount,
            String makerRemainingQuantity
    ) {
        EngineResult.Fill fill = result.fills().get(index);

        assertThat(fill.makerOrderKey()).isEqualTo(makerOrderKey);
        assertThat(fill.price()).isEqualByComparingTo(price);
        assertThat(fill.executedQuantity()).isEqualByComparingTo(executedQuantity);
        assertThat(fill.executedQuoteAmount()).isEqualByComparingTo(executedQuoteAmount);
        assertThat(fill.makerRemainingQuantity()).isEqualByComparingTo(makerRemainingQuantity);
    }

    private void assertSingleMatchDelta(
            EngineResult.PlaceResult result,
            OrderSide matchedSide,
            String matchedPrice,
            String beforeQuantity
    ) {
        EngineResult.BookDelta bookDelta = result.bookDeltas().getFirst();
        InMemoryOrderBook.LevelDelta levelDelta = bookDelta.delta();

        assertThat(bookDelta.reason()).isEqualTo(EngineResult.BookDeltaReason.MATCH_EXECUTED);
        assertThat(levelDelta.side()).isEqualTo(matchedSide);
        assertThat(levelDelta.price()).isEqualByComparingTo(matchedPrice);
        assertThat(levelDelta.before().totalQty()).isEqualByComparingTo(beforeQuantity);
        assertThat(levelDelta.before().orderCount()).isEqualTo(1);
        assertThat(levelDelta.after().totalQty()).isEqualByComparingTo("0");
        assertThat(levelDelta.after().orderCount()).isZero();
    }

    private void assertMatchDeltaAt(
            EngineResult.PlaceResult result,
            int index,
            OrderSide matchedSide,
            String matchedPrice,
            String beforeQuantity,
            int beforeOrderCount,
            String afterQuantity,
            int afterOrderCount
    ) {
        EngineResult.BookDelta bookDelta = result.bookDeltas().get(index);
        InMemoryOrderBook.LevelDelta levelDelta = bookDelta.delta();

        assertThat(bookDelta.reason()).isEqualTo(EngineResult.BookDeltaReason.MATCH_EXECUTED);
        assertThat(levelDelta.side()).isEqualTo(matchedSide);
        assertThat(levelDelta.price()).isEqualByComparingTo(matchedPrice);
        assertThat(levelDelta.before().totalQty()).isEqualByComparingTo(beforeQuantity);
        assertThat(levelDelta.before().orderCount()).isEqualTo(beforeOrderCount);
        assertThat(levelDelta.after().totalQty()).isEqualByComparingTo(afterQuantity);
        assertThat(levelDelta.after().orderCount()).isEqualTo(afterOrderCount);
    }

    private void assertOpenWithoutExecution(EngineResult.PlaceResult result, String remainingQuantity) {
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(result.executedQuantity()).isEqualByComparingTo("0");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("0");
        assertThat(result.remainingQuantity()).isEqualByComparingTo(remainingQuantity);
        assertThat(result.unlockAmount()).isEqualByComparingTo("0");
        assertThat(result.cancelReason()).isNull();
        assertThat(result.fills()).isEmpty();
        assertThat(result.bookDeltas()).hasSize(1);
    }

    private void assertSingleRestingDelta(
            EngineResult.PlaceResult result,
            OrderSide restingSide,
            String restingPrice,
            String remainingQuantity
    ) {
        EngineResult.BookDelta bookDelta = result.bookDeltas().getFirst();
        InMemoryOrderBook.LevelDelta levelDelta = bookDelta.delta();

        assertThat(bookDelta.reason()).isEqualTo(EngineResult.BookDeltaReason.RESTING_ORDER_ADDED);
        assertThat(levelDelta.side()).isEqualTo(restingSide);
        assertThat(levelDelta.price()).isEqualByComparingTo(restingPrice);
        assertThat(levelDelta.before().totalQty()).isEqualByComparingTo("0");
        assertThat(levelDelta.before().orderCount()).isZero();
        assertThat(levelDelta.after().totalQty()).isEqualByComparingTo(remainingQuantity);
        assertThat(levelDelta.after().orderCount()).isEqualTo(1);
    }

    private void assertRestingDeltaAt(
            EngineResult.PlaceResult result,
            int index,
            OrderSide restingSide,
            String restingPrice,
            String beforeQuantity,
            int beforeOrderCount,
            String afterQuantity,
            int afterOrderCount
    ) {
        EngineResult.BookDelta bookDelta = result.bookDeltas().get(index);
        InMemoryOrderBook.LevelDelta levelDelta = bookDelta.delta();

        assertThat(bookDelta.reason()).isEqualTo(EngineResult.BookDeltaReason.RESTING_ORDER_ADDED);
        assertThat(levelDelta.side()).isEqualTo(restingSide);
        assertThat(levelDelta.price()).isEqualByComparingTo(restingPrice);
        assertThat(levelDelta.before().totalQty()).isEqualByComparingTo(beforeQuantity);
        assertThat(levelDelta.before().orderCount()).isEqualTo(beforeOrderCount);
        assertThat(levelDelta.after().totalQty()).isEqualByComparingTo(afterQuantity);
        assertThat(levelDelta.after().orderCount()).isEqualTo(afterOrderCount);
    }

    private void assertRestingOrder(
            InMemoryOrderBook orderBook,
            String orderKey,
            OrderSide side,
            String price,
            String quantity
    ) {
        assertThat(orderBook.findOrder(orderKey)).isPresent();
        assertThat(orderBook.findOrder(orderKey).orElseThrow().getRemainingQty())
                .isEqualByComparingTo(quantity);

        PriceLevel.Snapshot levelSnapshot = requiredLevelSnapshot(orderBook, side, decimal(price));
        assertThat(levelSnapshot.totalQty()).isEqualByComparingTo(quantity);
        assertThat(levelSnapshot.orderCount()).isEqualTo(1);
    }

    private void assertCanceledWithoutExecution(
            EngineResult.PlaceResult result,
            String remainingQuantity,
            String unlockAmount,
            EngineResult.CancelReason cancelReason
    ) {
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(result.executedQuantity()).isEqualByComparingTo("0");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("0");
        if (remainingQuantity == null) {
            assertThat(result.remainingQuantity()).isNull();
        } else {
            assertThat(result.remainingQuantity()).isEqualByComparingTo(remainingQuantity);
        }
        assertThat(result.unlockAmount()).isEqualByComparingTo(unlockAmount);
        assertThat(result.cancelReason()).isEqualTo(cancelReason);
        assertThat(result.fills()).isEmpty();
        assertThat(result.bookDeltas()).isEmpty();
    }

    private CommandMessage.Place limitBid(String price, String quantity) {
        return limitBid(price, quantity, quoteAssetScale);
    }

    private CommandMessage.Place limitBid(String price, String quantity, int scaleQuote) {
        return createPlaceMessage(
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                price,
                quantity,
                null,
                baseAssetScale,
                scaleQuote
        );
    }

    private CommandMessage.Place limitAsk(String price, String quantity) {
        return createPlaceMessage(
                OrderSide.ASK,
                OrderType.LIMIT,
                TimeInForce.GTC,
                price,
                quantity,
                null,
                baseAssetScale,
                quoteAssetScale
        );
    }

    private CommandMessage.Place marketBid(String quoteAmount) {
        return marketBid(quoteAmount, baseAssetScale, quoteAssetScale);
    }

    private CommandMessage.Place marketBid(String quoteAmount, int scaleBase, int scaleQuote) {
        return createPlaceMessage(
                OrderSide.BID,
                OrderType.MARKET,
                TimeInForce.IOC,
                null,
                null,
                quoteAmount,
                scaleBase,
                scaleQuote
        );
    }

    private CommandMessage.Place marketAsk(String quantity) {
        return createPlaceMessage(
                OrderSide.ASK,
                OrderType.MARKET,
                TimeInForce.IOC,
                null,
                quantity,
                null,
                baseAssetScale,
                quoteAssetScale
        );
    }

    private CommandMessage.Place createPlaceMessage(
            OrderSide orderSide,
            OrderType orderType,
            TimeInForce timeInForce,
            String price,
            String quantity,
            String quoteAmount,
            int scaleBase,
            int scaleQuote
    ) {
        long commandLogId = nextCommandLogId++;

        return new CommandMessage.Place(
                commandLogId,
                userId,
                marketId,
                marketCode,
                "cid-" + commandLogId,
                orderKey(commandLogId),
                orderSide,
                orderType,
                timeInForce,
                decimalOrNull(price),
                decimalOrNull(quantity),
                decimalOrNull(quoteAmount),
                scaleBase,
                scaleQuote
        );
    }

    private InMemoryOrderBook orderBook(BookOrderEntry... entries) {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        for (BookOrderEntry entry : entries) {
            orderBook.add(entry);
        }
        return orderBook;
    }

    private static BookOrderEntry maker(Long orderId, OrderSide side, String price, String quantity) {
        return BookOrderEntry.create(orderKey(orderId), userId(orderId), side, decimal(price), decimal(quantity));
    }

    private static String orderKey(Long orderId) {
        return "order-key-" + orderId;
    }

    private static Long userId(Long orderId) {
        return 1_000_000L + orderId;
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static BigDecimal decimalOrNull(String value) {
        return value == null ? null : decimal(value);
    }

    private static PriceLevel.Snapshot requiredLevelSnapshot(
            InMemoryOrderBook orderBook,
            OrderSide side,
            BigDecimal price
    ) {
        return orderBook.getLevelSnapshot(side, price)
                .orElseThrow(() -> new AssertionError("expected level snapshot to exist"));
    }
}
