package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketWorkerManager {

    private final ConcurrentHashMap<Long, MarketWorker> workers = new ConcurrentHashMap<>();
    private volatile boolean accepting = true;

    public void submit(CommandMessage message) {
        if (!accepting) {
            throw new IllegalStateException("MarketWorkerManager는 종료 중이거나 종료되었습니다.");
        }

        MarketWorker worker = workers.computeIfAbsent(
                message.marketId(),
                MarketWorker::new
        );
        worker.start();
        worker.enqueue(message);
    }

    @PreDestroy
    public synchronized void shutdownAll() {
        if (!accepting) {
            return;
        }

        accepting = false;
        workers.values().forEach(MarketWorker::shutdown);
        workers.clear();
    }

    MarketWorker workerFor(Long marketId) {
        return workers.get(marketId);
    }

    int workerCount() {
        return workers.size();
    }
}
