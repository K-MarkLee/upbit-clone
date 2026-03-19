package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MarketWorker {

    private final Long marketId;
    private final BlockingQueue<CommandMessage> mailbox = new LinkedBlockingQueue<>();

    public MarketWorker(Long marketId) {
        this.marketId = marketId;
    }

    public void enqueue(CommandMessage message) {
        if (!marketId.equals(message.marketId())) {
            throw new IllegalArgumentException("message marketId does not match worker marketId");
        }
        mailbox.add(message);
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
}
