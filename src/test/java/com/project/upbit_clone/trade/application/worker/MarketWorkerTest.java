package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.EngineResult;
import com.project.upbit_clone.trade.application.engine.MatchingEngineCore;
import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.application.projector.EventProjector;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@DisplayName("MarketWorker 단위 테스트")
class MarketWorkerTest {

    private MarketWorker marketWorker;
    private WorkerWriteService workerWriteService;
    private EventProjector eventProjector;

    @BeforeEach
    void setUp() {
        workerWriteService = Mockito.mock(WorkerWriteService.class);
        eventProjector = Mockito.mock(EventProjector.class);
        marketWorker = new MarketWorker(100L, new MatchingEngineCore(), workerWriteService, eventProjector);
    }

    @AfterEach
    void tearDown() {
        marketWorker.shutdown();
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
                "order-key-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                null,
                8,
                8
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
                "order-key-2",
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
                "order-key-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                null,
                new BigDecimal("1"),
                null,
                8,
                8
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
                "order-key-1",
                OrderSide.BID,
                OrderType.MARKET,
                TimeInForce.IOC,
                null,
                null,
                null,
                8,
                8
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("market bid message 필수값이 누락되어 있습니다.");
    }

    @Test
    @DisplayName("Negative : limit 주문은 quoteAmount를 허용하지 않는다.")
    void reject_limit_order_with_quote_amount() {
        // given
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                "order-key-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                new BigDecimal("1000"),
                8,
                8
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit 주문은 quoteAmount를 허용하지 않습니다.");
    }

    @Test
    @DisplayName("Negative : market bid 주문은 price와 quantity를 허용하지 않는다.")
    void reject_market_bid_with_forbidden_fields() {
        // given
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                "order-key-1",
                OrderSide.BID,
                OrderType.MARKET,
                TimeInForce.IOC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                new BigDecimal("1000"),
                8,
                8
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("market bid 주문은 price와 quantity를 허용하지 않습니다.");
    }

    @Test
    @DisplayName("Negative : market ask 주문은 price와 quoteAmount를 허용하지 않는다.")
    void reject_market_ask_with_forbidden_fields() {
        // given
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                "order-key-1",
                OrderSide.ASK,
                OrderType.MARKET,
                TimeInForce.IOC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                new BigDecimal("1000"),
                8,
                8
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("market ask 주문은 price와 quoteAmount를 허용하지 않습니다.");
    }

    @Test
    @DisplayName("Negative : limit 주문은 GTC만 허용한다.")
    void reject_limit_order_with_non_gtc_tif() {
        // given
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                "order-key-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.IOC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                null,
                8,
                8
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit 주문은 GTC만 허용합니다.");
    }

    @Test
    @DisplayName("Negative : market 주문은 IOC만 허용한다.")
    void reject_market_order_with_non_ioc_tif() {
        // given
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                "order-key-1",
                OrderSide.BID,
                OrderType.MARKET,
                TimeInForce.GTC,
                null,
                null,
                new BigDecimal("1000"),
                8,
                8
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("market 주문은 IOC만 허용합니다.");
    }

    @Test
    @DisplayName("Negative : 양수 조건을 만족하지 않으면 적재할 수 없다.")
    void reject_place_order_with_non_positive_amount() {
        // given
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                "order-key-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                BigDecimal.ZERO,
                new BigDecimal("1"),
                null,
                8,
                8
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit 주문의 price는 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("Negative : baseAssetScale은 0 이상 8 이하여야 한다.")
    void reject_place_order_with_negative_base_asset_scale() {
        // given
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                "order-key-1",
                OrderSide.BID,
                OrderType.MARKET,
                TimeInForce.IOC,
                null,
                null,
                new BigDecimal("1000"),
                -1,
                8
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("baseAssetScale은 0 이상 8 이하여야 합니다.");
    }

    @Test
    @DisplayName("Negative : quoteAssetScale은 0 이상 8 이하여야 한다.")
    void reject_place_order_with_negative_quote_asset_scale() {
        // given
        CommandMessage.Place message = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                "order-key-1",
                OrderSide.BID,
                OrderType.MARKET,
                TimeInForce.IOC,
                null,
                null,
                new BigDecimal("1000"),
                8,
                -1
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quoteAssetScale은 0 이상 8 이하여야 합니다.");
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
                "order-key-2",
                null
        );

        // when & then
        assertThatThrownBy(() -> marketWorker.enqueue(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("message 필수값이 누락되어 있습니다.");
    }

    @Test
    @DisplayName("Happy : worker가 place 메시지를 consume하면 matching engine을 호출한다.")
    void worker_invokes_matching_engine_when_place_message_is_consumed() throws InterruptedException {
        // given
        CapturingMatchingEngineCore matchingEngineCore = new CapturingMatchingEngineCore();
        marketWorker = new MarketWorker(100L, matchingEngineCore, workerWriteService, eventProjector);
        CommandMessage.Place message = validLimitPlaceMessage();

        // when
        marketWorker.start();
        marketWorker.enqueue(message);

        // then
        assertThat(matchingEngineCore.awaitInvocation()).isTrue();
        assertThat(matchingEngineCore.lastMessage).isEqualTo(message);
    }

    @Test
    @DisplayName("Happy : worker가 cancel 메시지를 consume하면 targetOrderKey 주문을 오더북에서 제거한다.")
    void worker_removes_resting_order_when_cancel_message_is_consumed() throws InterruptedException {
        // given
        CommandMessage.Place placeMessage = validLimitPlaceMessage();
        CommandMessage.Cancel cancelMessage = new CommandMessage.Cancel(
                2L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                placeMessage.orderKey(),
                "USER_REQUEST"
        );

        // when
        marketWorker.start();
        marketWorker.enqueue(placeMessage);
        awaitOrderPresence(placeMessage.orderKey());
        marketWorker.enqueue(cancelMessage);

        // then
        awaitOrderAbsence(placeMessage.orderKey());
        Mockito.verify(workerWriteService, Mockito.timeout(1_000))
                .writeCancel(eq(cancelMessage), any(InMemoryOrderBook.LevelDelta.class));
    }

    private CommandMessage.Place validLimitPlaceMessage() {
        return new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-1",
                "order-key-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                null,
                8,
                8
        );
    }

    private static final class CapturingMatchingEngineCore extends MatchingEngineCore {
        private final CountDownLatch invocationLatch = new CountDownLatch(1);
        private volatile CommandMessage.Place lastMessage;

        @Override
        public EngineResult.PlaceResult place(
                CommandMessage.Place message,
                com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook orderBook
        ) {
            this.lastMessage = message;
            invocationLatch.countDown();
            return EngineResult.PlaceResult.open(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    message.quantity(),
                    BigDecimal.ZERO,
                    List.of(),
                    List.of()
            );
        }

        boolean awaitInvocation() throws InterruptedException {
            return invocationLatch.await(1, TimeUnit.SECONDS);
        }
    }

    private void awaitOrderPresence(String orderKey) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (System.nanoTime() < deadlineNanos) {
            if (currentOrderBook().findOrder(orderKey).isPresent()) {
                return;
            }
            Thread.sleep(10);
        }
        assertThat(currentOrderBook().findOrder(orderKey)).isPresent();
    }

    private void awaitOrderAbsence(String orderKey) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (System.nanoTime() < deadlineNanos) {
            if (currentOrderBook().findOrder(orderKey).isEmpty()) {
                return;
            }
            Thread.sleep(10);
        }
        assertThat(currentOrderBook().findOrder(orderKey)).isEmpty();
    }

    private InMemoryOrderBook currentOrderBook() {
        try {
            java.lang.reflect.Field field = MarketWorker.class.getDeclaredField("orderBook");
            field.setAccessible(true);
            return (InMemoryOrderBook) field.get(marketWorker);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("MarketWorker orderBook 필드 조회 실패", e);
        }
    }
}
