package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MarketWorkerManager 단위 테스트")
class MarketWorkerManagerTest {

    private static final Long BTC_MARKET_ID = 100L;
    private static final Long ETH_MARKET_ID = 200L;

    private MarketWorkerManager marketWorkerManager;
    private CommandMessage.Place btcPlaceMessage;
    private CommandMessage.Cancel btcCancelMessage;
    private CommandMessage.Place ethPlaceMessage;

    @BeforeEach
    void setUp() {
        marketWorkerManager = new MarketWorkerManager();
        btcPlaceMessage = new CommandMessage.Place(
                1L,
                10L,
                BTC_MARKET_ID,
                "KRW-BTC",
                "cid-btc-place",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                null
        );
        btcCancelMessage = new CommandMessage.Cancel(
                2L,
                10L,
                BTC_MARKET_ID,
                "KRW-BTC",
                "cid-btc-cancel",
                "USER_REQUEST"
        );
        ethPlaceMessage = new CommandMessage.Place(
                3L,
                11L,
                ETH_MARKET_ID,
                "KRW-ETH",
                "cid-eth-place",
                OrderSide.ASK,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("2000"),
                new BigDecimal("2"),
                null
        );
    }

    @AfterEach
    void tearDown() {
        marketWorkerManager.shutdownAll();
    }

    @Test
    @DisplayName("같은 marketId 메시지는 같은 worker로 라우팅된다.")
    void submit_routes_same_market_to_same_worker() {
        // when
        marketWorkerManager.submit(btcPlaceMessage);
        MarketWorker firstWorker = marketWorkerManager.workerFor(BTC_MARKET_ID);
        marketWorkerManager.submit(btcCancelMessage);
        MarketWorker secondWorker = marketWorkerManager.workerFor(BTC_MARKET_ID);

        // then
        assertThat(firstWorker).isNotNull();
        assertThat(secondWorker).isSameAs(firstWorker);
        assertThat(marketWorkerManager.workerCount()).isEqualTo(1);
        assertThat(firstWorker.marketId()).isEqualTo(BTC_MARKET_ID);
        assertThat(firstWorker.isRunning()).isTrue();
    }

    @Test
    @DisplayName("다른 marketId 메시지는 서로 다른 worker로 분리된다.")
    void submit_routes_different_markets_to_different_workers() {
        // when
        marketWorkerManager.submit(btcPlaceMessage);
        marketWorkerManager.submit(ethPlaceMessage);

        // then
        MarketWorker btcWorker = marketWorkerManager.workerFor(BTC_MARKET_ID);
        MarketWorker ethWorker = marketWorkerManager.workerFor(ETH_MARKET_ID);

        assertThat(marketWorkerManager.workerCount()).isEqualTo(2);
        assertThat(btcWorker).isNotNull();
        assertThat(ethWorker).isNotNull();
        assertThat(btcWorker).isNotSameAs(ethWorker);
        assertThat(btcWorker.marketId()).isEqualTo(BTC_MARKET_ID);
        assertThat(ethWorker.marketId()).isEqualTo(ETH_MARKET_ID);
        assertThat(btcWorker.isRunning()).isTrue();
        assertThat(ethWorker.isRunning()).isTrue();
    }

    @Test
    @DisplayName("shutdownAll은 worker를 종료하고 제거한다.")
    void shutdownAll_stops_and_clears_workers() {
        // given
        marketWorkerManager.submit(btcPlaceMessage);
        marketWorkerManager.submit(ethPlaceMessage);
        MarketWorker btcWorker = marketWorkerManager.workerFor(BTC_MARKET_ID);
        MarketWorker ethWorker = marketWorkerManager.workerFor(ETH_MARKET_ID);

        // when
        marketWorkerManager.shutdownAll();

        // then
        assertThat(btcWorker).isNotNull();
        assertThat(ethWorker).isNotNull();
        assertThat(btcWorker.isRunning()).isFalse();
        assertThat(ethWorker.isRunning()).isFalse();
        assertThat(marketWorkerManager.workerCount()).isZero();
        assertThat(marketWorkerManager.workerFor(BTC_MARKET_ID)).isNull();
        assertThat(marketWorkerManager.workerFor(ETH_MARKET_ID)).isNull();
    }

    @Test
    @DisplayName("shutdownAll 이후에는 새 메시지를 받지 않는다.")
    void submit_rejects_when_manager_is_shutdown() {
        // given
        marketWorkerManager.shutdownAll();

        // when & then
        assertThatThrownBy(() -> marketWorkerManager.submit(btcPlaceMessage))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MarketWorkerManager는 종료 중이거나 종료되었습니다.");
    }
}
