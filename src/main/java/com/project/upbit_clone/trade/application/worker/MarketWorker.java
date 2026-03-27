package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MarketWorker {

    private final Long marketId;
    private final BlockingQueue<CommandMessage> mailbox = new LinkedBlockingQueue<>();

    public MarketWorker(Long marketId) {
        this.marketId = Objects.requireNonNull(marketId, "marketId must not be null");
    }

    // 메시지 적제
    public void enqueue(CommandMessage message) {
        validateMessage(message);
        if (!marketId.equals(message.marketId())) {
            throw new IllegalArgumentException("message의 marketId가 worker의 marketId와 다릅니다.");
        }
        mailbox.add(message);
    }

    // 메시지 검증
    private void validateMessage(CommandMessage message) {
        if (message == null
                || message.commandLogId() == null
                || message.userId() == null
                || message.marketId() == null
                || message.clientOrderId() == null
                || message.clientOrderId().isBlank()
                || message.commandType() == null) {
            throw new IllegalArgumentException("message 필수값이 누락되었습니다.");
        }

        // limit 과 market에 따른 검증
        if (message instanceof CommandMessage.Place place) {
            validatePlace(place);
        }
    }

    // place검증
    private void validatePlace(CommandMessage.Place message) {
        if (message.orderSide() == null || message.orderType() == null) {
            throw new IllegalArgumentException("place message 필수값이 누락되었습니다.");
        }

        if (message.orderType() == OrderType.LIMIT) {
            if (message.price() == null || message.quantity() == null) {
                throw new IllegalArgumentException("limit place message 필수값이 누락되었습니다.");
            }
            return;
        }

        if (message.orderType() == OrderType.MARKET) {
            if (message.orderSide() == OrderSide.BID && message.quoteAmount() == null) {
                throw new IllegalArgumentException("market bid message 필수값이 누락되었습니다.");
            }
            if (message.orderSide() == OrderSide.ASK && message.quantity() == null) {
                throw new IllegalArgumentException("market ask message 필수값이 누락되었습니다.");
            }
            return;
        }

        throw new IllegalArgumentException("지원하지 않는 orderType 입니다.");
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
