package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MarketWorker 단위 테스트")
class MarketWorkerTest {

    private MarketWorker marketWorker;

    @BeforeEach
    void setUp() {
        marketWorker = new MarketWorker(100L);
    }

    @Test
    @DisplayName("Happy : 유효한 limit place 메시지는 mailbox에 적재된다.")
    void enqueue_valid_limit_place_message() {
        // given
        CommandMessage.Place message = validLimitPlaceMessage();

        // when
        marketWorker.enqueue(message);

        // then
        assertThat(marketWorker.pendingCount()).isEqualTo(1);
        assertThat(marketWorker.peek()).isEqualTo(message);
    }

    @Test
    @DisplayName("Negative : null 메시지는 적재할 수 없다.")
    void reject_null_message() {
        assertThatThrownBy(() -> marketWorker.enqueue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message 필수값이 누락되어 있습니다.");
    }

    @Test
    @DisplayName("Negative : 다른 marketId 메시지는 적재할 수 없다.")
    void reject_message_with_different_market_id() {
        // given
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                200L,
                "KRW-ETH",
                "cid-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                null
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message의 marketId가 worker의 marketId와 다릅니다.");
    }

    @Test
    @DisplayName("Negative : 같은 marketId라도 다른 marketCode 메시지는 적재할 수 없다.")
    void reject_message_with_different_market_code() {
        // given
        marketWorker.enqueue(validLimitPlaceMessage());

        CommandMessage.Cancel message = new CommandMessage.Cancel(
                2L,
                10L,
                100L,
                "KRW-ETH",
                "cid-2",
                null
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message의 marketCode가 worker의 marketCode와 다릅니다.");
    }

    @Test
    @DisplayName("Negative : limit 주문은 price와 quantity가 모두 필요하다.")
    void reject_limit_order_without_required_fields() {
        // given
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                null,
                new BigDecimal("1"),
                null
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit place message 필수값이 누락되어 있습니다.");
    }

    @Test
    @DisplayName("Negative : market bid 주문은 quoteAmount가 필요하다.")
    void reject_market_bid_without_quote_amount() {
        // given
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
                null
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("market bid message 필수값이 누락되어 있습니다.");
    }

    @Test
    @DisplayName("Negative : 공통 필수값이 없으면 적재할 수 없다.")
    void reject_message_without_common_required_fields() {
        // given
        CommandMessage.Cancel message = new CommandMessage.Cancel(
                2L,
                10L,
                100L,
                "KRW-BTC",
                " ",
                null
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message 필수값이 누락되어 있습니다.");
    }

    private CommandMessage.Place validLimitPlaceMessage() {
        return new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                null
        );
    }
}
