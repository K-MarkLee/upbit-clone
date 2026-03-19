package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketWorkerManager {

    private final ConcurrentHashMap<Long, MarketWorker> workers = new ConcurrentHashMap<>();

    public void submit(CommandMessage message) {
        MarketWorker worker = workers.computeIfAbsent(
                message.marketId(),
                MarketWorker::new
        );
        worker.enqueue(message);
    }

    MarketWorker workerFor(Long marketId) {
        return workers.get(marketId);
    }

    int workerCount() {
        return workers.size();
    }
}
