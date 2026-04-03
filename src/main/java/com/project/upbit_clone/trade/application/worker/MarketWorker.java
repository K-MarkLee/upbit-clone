package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.EngineResult;
import com.project.upbit_clone.trade.application.engine.MatchingEngineCore;
import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MarketWorker {

    private static final Logger log = LoggerFactory.getLogger(MarketWorker.class);

    private final Long marketId;
    private final MatchingEngineCore matchingEngineCore;
    private final InMemoryOrderBook orderBook;
    private final BlockingQueue<CommandMessage> mailbox = new LinkedBlockingQueue<>();
    private volatile String marketCode;
    private volatile boolean running;
    private Thread workerThread;

    public MarketWorker(Long marketId, MatchingEngineCore matchingEngineCore) {
        this.marketId = Objects.requireNonNull(marketId, "marketId는 null값일 수 없습니다.");
        this.matchingEngineCore = Objects.requireNonNull(matchingEngineCore, "matchingEngineCore는 null값일 수 없습니다.");
        this.orderBook = new InMemoryOrderBook();
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;
        workerThread = Thread.ofVirtual()
                .name("market-worker-" + marketId)
                .start(this::runLoop);
    }

    public synchronized void shutdown() {
        if (!running) {
            return;
        }

        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    // 메시지 적제
    public void enqueue(CommandMessage message) {
        validateMessage(message);
        if (!marketId.equals(message.marketId())) {
            throw new IllegalArgumentException("message의 marketId가 worker의 marketId와 다릅니다.");
        }
        bindMarketCode(message.marketCode());
        mailbox.add(message);
    }

    private void runLoop() {
        while (running) {
            try {
                CommandMessage message = mailbox.take();
                dispatch(message);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                running = false;
                log.info(
                        "MarketWorker가 인터럽트되어 종료합니다. marketId={}, marketCode={}",
                        marketId,
                        marketCode,
                        exception
                );
                return;
            } catch (RuntimeException exception) {
                log.error(
                        "MarketWorker 메시지 처리에 실패했습니다. marketId={}, marketCode={}",
                        marketId,
                        marketCode,
                        exception
                );
            }
        }
    }

    private void dispatch(CommandMessage message) {
        switch (message) {
            case CommandMessage.Place place -> handlePlace(place);
            case CommandMessage.Cancel cancel -> handleCancel(cancel);
        }
    }

    private void handlePlace(CommandMessage.Place message) {
        EngineResult.PlaceResult result = matchingEngineCore.place(message, orderBook);
        log.debug(
                "주문(place) 처리 결과: marketId={}, marketCode={}, commandLogId={}, takerStatus={}, remainingQuantity={}, fillCount={}",
                marketId,
                marketCode,
                message.commandLogId(),
                result.takerStatus(),
                result.remainingQuantity(),
                result.fills().size()
        );
    }

    private void handleCancel(CommandMessage.Cancel message) {
        boolean removed = orderBook.remove(message.targetOrderKey()).isPresent();
        log.debug(
                "주문(cancel) 처리 결과: marketId={}, marketCode={}, commandLogId={}, targetOrderKey={}, removed={}",
                marketId,
                marketCode,
                message.commandLogId(),
                message.targetOrderKey(),
                removed
        );
    }

    private synchronized void bindMarketCode(String marketCode) {
        if (this.marketCode == null) {
            this.marketCode = marketCode;
            return;
        }

        if (!this.marketCode.equals(marketCode)) {
            throw new IllegalArgumentException("message의 marketCode가 worker의 marketCode와 다릅니다.");
        }
    }

    // 메시지 검증
    private void validateMessage(CommandMessage message) {
        if (message == null
                || message.commandLogId() == null
                || message.userId() == null
                || message.marketId() == null
                || message.marketCode() == null
                || message.marketCode().isBlank()
                || message.clientOrderId() == null
                || message.clientOrderId().isBlank()
                || message.commandType() == null) {
            throw new IllegalArgumentException("message 필수값이 누락되어 있습니다.");
        }

        // limit 과 market에 따른 검증
        if (message instanceof CommandMessage.Place place) {
            validatePlace(place);
            return;
        }
        if (message instanceof CommandMessage.Cancel cancel) {
            validateCancel(cancel);
        }
    }

    // place 검증
    private void validatePlace(CommandMessage.Place message) {
        if (message.orderKey() == null
                || message.orderKey().isBlank()
                || message.orderSide() == null
                || message.orderType() == null) {
            throw new IllegalArgumentException("place message 필수값이 누락되어 있습니다.");
        }

        if (message.orderType() == OrderType.LIMIT) {
            if (message.price() == null || message.quantity() == null) {
                throw new IllegalArgumentException("limit place message 필수값이 누락되어 있습니다.");
            }
            return;
        }

        if (message.orderType() == OrderType.MARKET) {
            if (message.orderSide() == OrderSide.BID && message.quoteAmount() == null) {
                throw new IllegalArgumentException("market bid message 필수값이 누락되어 있습니다.");
            }
            if (message.orderSide() == OrderSide.ASK && message.quantity() == null) {
                throw new IllegalArgumentException("market ask message 필수값이 누락되어 있습니다.");
            }
            return;
        }

        throw new IllegalArgumentException("지원하지 않는 orderType 입니다.");
    }

    private void validateCancel(CommandMessage.Cancel message) {
        if (message.targetOrderKey() == null || message.targetOrderKey().isBlank()) {
            throw new IllegalArgumentException("cancel message 필수값이 누락되어 있습니다.");
        }
    }

    Long marketId() {
        return marketId;
    }

    int pendingCount() {
        return mailbox.size();
    }

    CommandMessage peek() {
        return mailbox.peek();
    }

    boolean isRunning() {
        return running;
    }
}
