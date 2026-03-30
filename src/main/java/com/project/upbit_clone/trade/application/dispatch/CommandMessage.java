package com.project.upbit_clone.trade.application.dispatch;

import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;

import java.math.BigDecimal;

public sealed interface CommandMessage permits CommandMessage.Place, CommandMessage.Cancel {

    Long commandLogId();
    Long userId();
    Long marketId();
    String marketCode();
    String clientOrderId();
    CommandType commandType();

    record Place(
            Long commandLogId,
            Long userId,
            Long marketId,
            String marketCode,
            String clientOrderId,
            OrderSide orderSide,
            OrderType orderType,
            TimeInForce timeInForce,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount
    ) implements CommandMessage {

        @Override
        public CommandType commandType() {
            return CommandType.PLACE_ORDER;
        }
    }

    record Cancel(
            Long commandLogId,
            Long userId,
            Long marketId,
            String marketCode,
            String clientOrderId,
            String cancelReason
    ) implements CommandMessage {

        @Override
        public CommandType commandType() {
            return CommandType.CANCEL_ORDER;
        }
    }
}
