package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MarketWorkerManager 단위 테스트")
class MarketWorkerManagerTest {

    private final MarketWorkerManager marketWorkerManager = new MarketWorkerManager();

    @Test
    @DisplayName("같은 marketId 메시지는 같은 worker mailbox에 적재된다.")
    void submit_routes_same_market_to_same_worker() {
        // given
        CommandMessage first = new CommandMessage.Place(
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
        CommandMessage second = new CommandMessage.Cancel(
                2L,
                10L,
                100L,
                "KRW-BTC",
                "cid-2",
                "USER_REQUEST"
        );

        // when
        marketWorkerManager.submit(first);
        marketWorkerManager.submit(second);

        // then
        MarketWorker worker = marketWorkerManager.workerFor(100L);
        assertThat(worker).isNotNull();
        assertThat(marketWorkerManager.workerCount()).isEqualTo(1);
        assertThat(worker.pendingCount()).isEqualTo(2);
        assertThat(worker.peek()).isEqualTo(first);
    }

    @Test
    @DisplayName("다른 marketId 메시지는 서로 다른 worker로 분리된다.")
    void submit_routes_different_markets_to_different_workers() {
        // given
        CommandMessage btc = new CommandMessage.Place(
                1L,
                10L,
                100L,
                "KRW-BTC",
                "cid-btc",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                null
        );
        CommandMessage eth = new CommandMessage.Place(
                2L,
                11L,
                200L,
                "KRW-ETH",
                "cid-eth",
                OrderSide.ASK,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("2000"),
                new BigDecimal("2"),
                null
        );

        // when
        marketWorkerManager.submit(btc);
        marketWorkerManager.submit(eth);

        // then
        assertThat(marketWorkerManager.workerCount()).isEqualTo(2);
        assertThat(marketWorkerManager.workerFor(100L)).isNotNull();
        assertThat(marketWorkerManager.workerFor(200L)).isNotNull();
        assertThat(marketWorkerManager.workerFor(100L).pendingCount()).isEqualTo(1);
        assertThat(marketWorkerManager.workerFor(200L).pendingCount()).isEqualTo(1);
    }
}
