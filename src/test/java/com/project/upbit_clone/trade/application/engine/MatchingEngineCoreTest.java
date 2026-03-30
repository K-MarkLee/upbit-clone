package com.project.upbit_clone.trade.application.engine;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.orderbook.BookOrderEntry;
import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MatchingEngineCore 단위 테스트")
class MatchingEngineCoreTest {

    private final MatchingEngineCore matchingEngineCore = new MatchingEngineCore();

    @Test
    @DisplayName("Happy : 반대편 호가가 없으면 LIMIT 주문은 OPEN 상태 결과를 반환한다.")
    void place_limit_without_opposite_book_returns_open() {
        // given
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("1000"),
                new BigDecimal("2"),
                null
        );

        // when
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(result.executedQuantity()).isEqualByComparingTo("0");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("0");
        assertThat(result.remainingQuantity()).isEqualByComparingTo("2");
        assertThat(result.unlockAmount()).isEqualByComparingTo("0");
        assertThat(result.cancelReason()).isNull();
        assertThat(result.fills()).isEmpty();
        assertThat(result.bookDeltas()).isEmpty();
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
        assertThat(result.cancelReason()).isEqualTo("NO_TRADE_STREAM");
        assertThat(result.fills()).isEmpty();
        assertThat(result.bookDeltas()).isEmpty();
    }

    @Test
    @DisplayName("Negative : 가격이 교차되면 아직 미구현 매칭 루프 예외를 반환한다.")
    void place_crossed_limit_throws_until_matching_loop_is_implemented() {
        // given
        InMemoryOrderBook orderBook = new InMemoryOrderBook();
        orderBook.add(BookOrderEntry.create(
                200L,
                OrderSide.ASK,
                new BigDecimal("900"),
                new BigDecimal("1")
        ));
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("1000"),
                new BigDecimal("2"),
                null
        );

        // when & then
        assertThatThrownBy(() -> matchingEngineCore.place(message, orderBook))
                .isInstanceOf(EngineException.class)
                .hasMessage("실제 매칭 루프는 아직 구현되지 않았습니다.");
    }
}
