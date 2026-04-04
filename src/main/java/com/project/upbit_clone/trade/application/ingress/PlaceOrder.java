package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.application.dispatch.CommandDispatcher;
import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

@Service
public class PlaceOrder extends AbstractOrderIngress<PlaceOrder.Command> {

    public PlaceOrder(
            UserRepository userRepository,
            MarketRepository marketRepository,
            JsonMapper jsonMapper,
            IdempotencyHitService idempotencyHitService,
            CommandLogAppendService commandLogAppendService,
            OrderCommandHashService orderCommandHashService,
            CommandDispatcher commandDispatcher
    ) {
        super(
                userRepository,
                marketRepository,
                jsonMapper,
                idempotencyHitService,
                commandLogAppendService,
                orderCommandHashService,
                commandDispatcher
        );
    }

    public CommandAck handle(Command command) {
        return handleInternal(command);
    }

    public record Command(
            Long userId,
            Long marketId,
            String clientOrderId,
            OrderSide orderSide,
            OrderType orderType,
            TimeInForce timeInForce,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount
    ) implements OrderCommand {
        @Override
        public CommandType commandType() {
            return CommandType.PLACE_ORDER;
        }
    }

    @Override
    protected void validateBusiness(Command command, Market market, User user, String commandId) {
        // TODO: 거래시 사용자의 지갑이 있는지 검증을 해야함.
        Order.create(new Order.CreateCommand(
                market,
                user,
                command.clientOrderId(),
                commandId,
                command.orderSide(),
                command.orderType(),
                command.timeInForce(),
                command.price(),
                command.quantity(),
                command.quoteAmount()
        ));
    }

    @Override
    protected CommandMessage toCommandMessage(Long commandLogId, String commandId, Command command, Market market) {
        return new CommandMessage.Place(
                commandLogId,
                command.userId(),
                command.marketId(),
                market.getMarketCode(),
                command.clientOrderId(),
                commandId,
                command.orderSide(),
                command.orderType(),
                command.timeInForce(),
                command.price(),
                command.quantity(),
                command.quoteAmount(),
                market.getBaseAsset().getDecimals().intValue()
        );
    }
}
