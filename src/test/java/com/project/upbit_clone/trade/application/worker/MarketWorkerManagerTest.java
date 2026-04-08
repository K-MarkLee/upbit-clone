package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.MatchingEngineCore;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MarketWorkerManager 단위 테스트")
class MarketWorkerManagerTest {

    private static final Long BTC_MARKET_ID = 100L;
    private static final Long ETH_MARKET_ID = 200L;

    private MarketWorkerManager marketWorkerManager;
    private WorkerWriteService workerWriteService;
    private CommandMessage.Place btcPlaceMessage;
    private CommandMessage.Cancel btcCancelMessage;
    private CommandMessage.Place ethPlaceMessage;

    @BeforeEach
    void setUp() {
        workerWriteService = new WorkerWriteService();
        marketWorkerManager = new MarketWorkerManager(new MatchingEngineCore(), workerWriteService);
        btcPlaceMessage = new CommandMessage.Place(
                1L,
                10L,
                BTC_MARKET_ID,
                "KRW-BTC",
                "cid-btc-place",
                "order-key-btc-place",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                null,
                8
        );
        btcCancelMessage = new CommandMessage.Cancel(
                2L,
                10L,
                BTC_MARKET_ID,
                "KRW-BTC",
                "cid-btc-cancel",
                "order-key-btc-place",
                "USER_REQUEST"
        );
        ethPlaceMessage = new CommandMessage.Place(
                3L,
                11L,
                ETH_MARKET_ID,
                "KRW-ETH",
                "cid-eth-place",
                "order-key-eth-place",
                OrderSide.ASK,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("2000"),
                new BigDecimal("2"),
                null,
                8
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

    @Test
    @DisplayName("submit과 shutdownAll은 같은 임계영역에서 직렬화된다.")
    void submit_and_shutdown_all_are_serialized() throws InterruptedException {
        // given
        BlockingCreateMarketWorkerManager blockingManager =
                new BlockingCreateMarketWorkerManager(new MatchingEngineCore(), workerWriteService);
        marketWorkerManager = blockingManager;
        CountDownLatch submitCompleted = new CountDownLatch(1);
        CountDownLatch shutdownCompleted = new CountDownLatch(1);

        Thread submitThread = Thread.ofVirtual().start(() -> {
            try {
                blockingManager.submit(btcPlaceMessage);
            } finally {
                submitCompleted.countDown();
            }
        });

        assertThat(blockingManager.awaitCreationStarted()).isTrue();

        Thread shutdownThread = Thread.ofVirtual().start(() -> {
            try {
                blockingManager.shutdownAll();
            } finally {
                shutdownCompleted.countDown();
            }
        });

        // then
        assertThat(shutdownCompleted.await(200, TimeUnit.MILLISECONDS)).isFalse();

        // when
        blockingManager.releaseCreation();

        assertThat(submitCompleted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(shutdownCompleted.await(1, TimeUnit.SECONDS)).isTrue();
        submitThread.join();
        shutdownThread.join();
        assertThat(marketWorkerManager.workerCount()).isZero();
        assertThatThrownBy(() -> marketWorkerManager.submit(btcPlaceMessage))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MarketWorkerManager는 종료 중이거나 종료되었습니다.");
    }

    private static final class BlockingCreateMarketWorkerManager extends MarketWorkerManager {
        private final MatchingEngineCore matchingEngineCore;
        private final WorkerWriteService workerWriteService;
        private final CountDownLatch creationStarted = new CountDownLatch(1);
        private final CountDownLatch allowCreation = new CountDownLatch(1);

        private BlockingCreateMarketWorkerManager(
                MatchingEngineCore matchingEngineCore,
                WorkerWriteService workerWriteService
        ) {
            super(matchingEngineCore, workerWriteService);
            this.matchingEngineCore = matchingEngineCore;
            this.workerWriteService = workerWriteService;
        }

        @Override
        MarketWorker createWorker(Long marketId) {
            creationStarted.countDown();
            awaitAllowCreation();
            return new MarketWorker(marketId, matchingEngineCore, workerWriteService);
        }

        private boolean awaitCreationStarted() throws InterruptedException {
            return creationStarted.await(1, TimeUnit.SECONDS);
        }

        private void releaseCreation() {
            allowCreation.countDown();
        }

        private void awaitAllowCreation() {
            try {
                if (!allowCreation.await(1, TimeUnit.SECONDS)) {
                    throw new AssertionError("worker 생성 해제가 시간 내에 일어나지 않았습니다.");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("worker 생성 대기 중 인터럽트가 발생했습니다.", exception);
            }
        }
    }
}
