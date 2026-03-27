package com.project.upbit_clone.trade.application.dispatch;

import com.project.upbit_clone.trade.application.worker.MarketWorkerManager;
import org.springframework.stereotype.Component;

@Component
public class LocalQueueCommandDispatcher implements CommandDispatcher {

    private final MarketWorkerManager marketWorkerManager;

    public LocalQueueCommandDispatcher(MarketWorkerManager marketWorkerManager) {
        this.marketWorkerManager = marketWorkerManager;
    }

    @Override
    public void dispatch(CommandMessage message) {
        marketWorkerManager.submit(message);
    }
}
