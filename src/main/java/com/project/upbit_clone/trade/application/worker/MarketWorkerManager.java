package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.MatchingEngineCore;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketWorkerManager {

    private final ConcurrentHashMap<Long, MarketWorker> workers = new ConcurrentHashMap<>();
    private final MatchingEngineCore matchingEngineCore;
    private final WorkerWriteService workerWriteService;
    private volatile boolean accepting = true;

    public MarketWorkerManager(MatchingEngineCore matchingEngineCore, WorkerWriteService workerWriteService) {
        this.matchingEngineCore = matchingEngineCore;
        this.workerWriteService = workerWriteService;
    }

    public synchronized void submit(CommandMessage message) {
        if (!accepting) {
            throw new IllegalStateException("MarketWorkerManager는 종료 중이거나 종료되었습니다.");
        }

        MarketWorker worker = workers.computeIfAbsent(
                message.marketId(),
                this::createWorker
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

    MarketWorker createWorker(Long marketId) {
        return new MarketWorker(marketId, matchingEngineCore, workerWriteService);
    }

    MarketWorker workerFor(Long marketId) {
        return workers.get(marketId);
    }

    int workerCount() {
        return workers.size();
    }
}
