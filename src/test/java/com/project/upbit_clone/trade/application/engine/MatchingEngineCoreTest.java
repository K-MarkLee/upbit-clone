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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MatchingEngineCore 단위 테스트")
class MatchingEngineCoreTest {

    private MatchingEngineCore matchingEngineCore;

    @BeforeEach
    void setUp() {
        matchingEngineCore = new MatchingEngineCore();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("limitGtcNoMatchCases")
    @DisplayName("Happy : LIMIT GTC 주문이 무체결이면 OPEN 상태로 resting 된다.")
    void place_limit_gtc_without_match_returns_open_and_adds_resting_order(
            String caseName,
            InMemoryOrderBook orderBook,
            CommandMessage.Place message,
            OrderSide expectedRestingSide,
            BigDecimal expectedRestingPrice,
            BigDecimal expectedRemainingQuantity
    ) {
        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);
        PriceLevel.Snapshot restingLevel = getRequiredLevelSnapshot(
                orderBook,
                expectedRestingSide,
                expectedRestingPrice
        );
        EngineResult.BookDelta bookDelta = result.bookDeltas().getFirst();
        InMemoryOrderBook.LevelDelta levelDelta = bookDelta.delta();

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(result.executedQuantity()).isEqualByComparingTo("0");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("0");
        assertThat(result.remainingQuantity()).isEqualByComparingTo(expectedRemainingQuantity);
        assertThat(result.unlockAmount()).isEqualByComparingTo("0");
        assertThat(result.cancelReason()).isNull();
        assertThat(result.fills()).isEmpty();
        assertThat(result.bookDeltas()).hasSize(1);
        assertThat(bookDelta.reason()).isEqualTo(EngineResult.BookDeltaReason.RESTING_ORDER_ADDED);
        assertThat(levelDelta.side()).isEqualTo(expectedRestingSide);
        assertThat(levelDelta.price()).isEqualByComparingTo(expectedRestingPrice);
        assertThat(levelDelta.before().totalQty()).isEqualByComparingTo("0");
        assertThat(levelDelta.before().orderCount()).isZero();
        assertThat(levelDelta.after().totalQty()).isEqualByComparingTo(expectedRemainingQuantity);
        assertThat(levelDelta.after().orderCount()).isEqualTo(1);
        assertThat(restingLevel.totalQty()).isEqualByComparingTo(expectedRemainingQuantity);
        assertThat(restingLevel.orderCount()).isEqualTo(1);
    }

    private static Stream<Arguments> limitGtcNoMatchCases() {
        return Stream.of(
                Arguments.of(
                        "반대편 호가가 없으면 BID LIMIT GTC는 resting 된다.",
                        new InMemoryOrderBook(),
                        createLimitPlaceMessage(1L, OrderSide.BID, "1000", "2"),
                        OrderSide.BID,
                        new BigDecimal("1000"),
                        new BigDecimal("2")
                ),
                Arguments.of(
                        "반대편 호가가 없으면 ASK LIMIT GTC는 resting 된다.",
                        new InMemoryOrderBook(),
                        createLimitPlaceMessage(2L, OrderSide.ASK, "1000", "2"),
                        OrderSide.ASK,
                        new BigDecimal("1000"),
                        new BigDecimal("2")
                ),
                Arguments.of(
                        "best ask가 더 높아 가격이 안 교차되면 BID LIMIT GTC는 resting 된다.",
                        orderBookWithLevel(201L, OrderSide.ASK, "1100", "1"),
                        createLimitPlaceMessage(3L, OrderSide.BID, "1000", "2"),
                        OrderSide.BID,
                        new BigDecimal("1000"),
                        new BigDecimal("2")
                ),
                Arguments.of(
                        "best bid가 더 낮아 가격이 안 교차되면 ASK LIMIT GTC는 resting 된다.",
                        orderBookWithLevel(202L, OrderSide.BID, "900", "1"),
                        createLimitPlaceMessage(4L, OrderSide.ASK, "1000", "2"),
                        OrderSide.ASK,
                        new BigDecimal("1000"),
                        new BigDecimal("2")
                )
        );
    }

    @Test
    @DisplayName("Happy : 반대편 호가가 없으면 MARKET 주문은 CANCELED 결과를 반환한다.")
    void place_market_without_opposite_book_returns_canceled() {
        // given
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                OrderSide.BID,
                OrderType.MARKET,
                TimeInForce.IOC,
                null,
                null,
                new BigDecimal("10000")
        );

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(result.executedQuantity()).isEqualByComparingTo("0");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("0");
        assertThat(result.remainingQuantity()).isNull();
        assertThat(result.unlockAmount()).isEqualByComparingTo("10000");
        assertThat(result.cancelReason()).isEqualTo(EngineResult.CancelReason.NO_TRADE_STREAM);
        assertThat(result.fills()).isEmpty();
        assertThat(result.bookDeltas()).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullRequiredPlaceInputs")
    @DisplayName("Negative : place 입력값이 null이면 NullPointerException을 반환한다.")
    void place_with_null_required_inputs(
            String caseName,
            CommandMessage.Place message,
            InMemoryOrderBook orderBook,
            String expectedMessage
    ) {
        // when & then
        assertThatThrownBy(() -> matchingEngineCore.place(message, orderBook))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(expectedMessage);
    }

    private static Stream<Arguments> nullRequiredPlaceInputs() {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        CommandMessage.Place message = createLimitPlaceMessage(1L, OrderSide.BID, "1000", "2");

        return Stream.of(
                Arguments.of("message null", null, orderBook, "message는 null일 수 없습니다."),
                Arguments.of("orderBook null", message, null, "orderBook은 null일 수 없습니다.")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("limitGtcSingleMakerFilledCases")
    @DisplayName("Happy : LIMIT GTC 주문이 단일 maker와 교차되면 FILLED 상태로 전량 체결된다.")
    void place_limit_gtc_with_single_maker_returns_filled_and_consumes_best_level(
            String caseName,
            InMemoryOrderBook orderBook,
            CommandMessage.Place message,
            Long expectedMakerOrderId,
            OrderSide expectedMatchedSide,
            BigDecimal expectedMatchedPrice,
            BigDecimal expectedExecutedQuantity,
            BigDecimal expectedExecutedQuoteAmount,
            BigDecimal expectedMakerRemainingQuantity,
            BigDecimal expectedLevelAfterQuantity,
            int expectedLevelAfterOrderCount
    ) {
        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);
        EngineResult.Fill fill = result.fills().getFirst();
        EngineResult.BookDelta bookDelta = result.bookDeltas().getFirst();
        InMemoryOrderBook.LevelDelta levelDelta = bookDelta.delta();

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.executedQuantity()).isEqualByComparingTo(expectedExecutedQuantity);
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo(expectedExecutedQuoteAmount);
        assertThat(result.remainingQuantity()).isEqualByComparingTo("0");
        assertThat(result.unlockAmount()).isEqualByComparingTo("0");
        assertThat(result.cancelReason()).isNull();
        assertThat(result.fills()).hasSize(1);
        assertThat(fill.makerOrderId()).isEqualTo(expectedMakerOrderId);
        assertThat(fill.price()).isEqualByComparingTo(expectedMatchedPrice);
        assertThat(fill.executedQuantity()).isEqualByComparingTo(expectedExecutedQuantity);
        assertThat(fill.executedQuoteAmount()).isEqualByComparingTo(expectedExecutedQuoteAmount);
        assertThat(fill.makerRemainingQuantity()).isEqualByComparingTo(expectedMakerRemainingQuantity);
        assertThat(result.bookDeltas()).hasSize(1);
        assertThat(bookDelta.reason()).isEqualTo(EngineResult.BookDeltaReason.MATCH_EXECUTED);
        assertThat(levelDelta.side()).isEqualTo(expectedMatchedSide);
        assertThat(levelDelta.price()).isEqualByComparingTo(expectedMatchedPrice);
        assertThat(levelDelta.before().totalQty()).isEqualByComparingTo(
                expectedExecutedQuantity.add(expectedMakerRemainingQuantity)
        );
        assertThat(levelDelta.before().orderCount()).isEqualTo(1);
        assertThat(levelDelta.after().totalQty()).isEqualByComparingTo(expectedLevelAfterQuantity);
        assertThat(levelDelta.after().orderCount()).isEqualTo(expectedLevelAfterOrderCount);

        if (expectedLevelAfterOrderCount == 0) {
            assertThat(orderBook.getLevelSnapshot(expectedMatchedSide, expectedMatchedPrice)).isEmpty();
            assertThat(orderBook.findOrder(expectedMakerOrderId)).isEmpty();
            return;
        }

        PriceLevel.Snapshot remainingLevel = getRequiredLevelSnapshot(
                orderBook,
                expectedMatchedSide,
                expectedMatchedPrice
        );
        assertThat(orderBook.findOrder(expectedMakerOrderId)).isPresent();
        assertThat(orderBook.findOrder(expectedMakerOrderId).orElseThrow().getRemainingQty())
                .isEqualByComparingTo(expectedMakerRemainingQuantity);
        assertThat(remainingLevel.totalQty()).isEqualByComparingTo(expectedLevelAfterQuantity);
        assertThat(remainingLevel.orderCount()).isEqualTo(expectedLevelAfterOrderCount);
    }

    private static Stream<Arguments> limitGtcSingleMakerFilledCases() {
        return Stream.of(
                Arguments.of(
                        "BID LIMIT GTC는 단일 ASK maker와 교차되면 maker 가격으로 전량 체결된다.",
                        orderBookWithLevel(301L, OrderSide.ASK, "900", "2"),
                        createLimitPlaceMessage(11L, OrderSide.BID, "1000", "2"),
                        301L,
                        OrderSide.ASK,
                        new BigDecimal("900"),
                        new BigDecimal("2"),
                        new BigDecimal("1800"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0
                ),
                Arguments.of(
                        "ASK LIMIT GTC는 단일 BID maker와 교차되면 maker 가격으로 전량 체결된다.",
                        orderBookWithLevel(302L, OrderSide.BID, "1100", "2"),
                        createLimitPlaceMessage(12L, OrderSide.ASK, "1000", "2"),
                        302L,
                        OrderSide.BID,
                        new BigDecimal("1100"),
                        new BigDecimal("2"),
                        new BigDecimal("2200"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0
                ),
                Arguments.of(
                        "BID LIMIT GTC는 단일 ASK maker의 일부만 체결해도 taker가 종료되면 FILLED다.",
                        orderBookWithLevel(303L, OrderSide.ASK, "900", "5"),
                        createLimitPlaceMessage(13L, OrderSide.BID, "1000", "2"),
                        303L,
                        OrderSide.ASK,
                        new BigDecimal("900"),
                        new BigDecimal("2"),
                        new BigDecimal("1800"),
                        new BigDecimal("3"),
                        new BigDecimal("3"),
                        1
                ),
                Arguments.of(
                        "ASK LIMIT GTC는 단일 BID maker의 일부만 체결해도 taker가 종료되면 FILLED다.",
                        orderBookWithLevel(304L, OrderSide.BID, "1100", "5"),
                        createLimitPlaceMessage(14L, OrderSide.ASK, "1000", "2"),
                        304L,
                        OrderSide.BID,
                        new BigDecimal("1100"),
                        new BigDecimal("2"),
                        new BigDecimal("2200"),
                        new BigDecimal("3"),
                        new BigDecimal("3"),
                        1
                )
        );
    }



    private static CommandMessage.Place createLimitPlaceMessage(
            Long commandLogId,
            OrderSide orderSide,
            String price,
            String quantity
    ) {
        return new CommandMessage.Place(
                commandLogId,
                10L,
                100L,
                "KRW-BTC",
                "cid-" + commandLogId,
                orderSide,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal(price),
                new BigDecimal(quantity),
                null
        );
    }

    private static InMemoryOrderBook orderBookWithLevel(
            Long orderId,
            OrderSide orderSide,
            String price,
            String quantity
    ) {
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        BookOrderEntry entry = BookOrderEntry.create(
                orderId,
                orderSide,
                new BigDecimal(price),
                new BigDecimal(quantity)
        );
        orderBook.add(entry);
        return orderBook;
    }

    private static PriceLevel.Snapshot getRequiredLevelSnapshot(
            InMemoryOrderBook orderBook,
            OrderSide side,
            BigDecimal price
    ) {
        return orderBook.getLevelSnapshot(side, price)
                .orElseThrow(() -> new AssertionError("expected level snapshot to exist"));
    }
}
