package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.application.dispatch.CommandDispatcher;
import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CancelOrder extends AbstractOrderIngress<CancelOrder.Command> {

    private final OrderRepository orderRepository;

    public CancelOrder(
            OrderRepository orderRepository,
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
        this.orderRepository = orderRepository;
    }

    public CommandAck handle(Command command) {
        return handleInternal(command);
    }

    public record Command(
            Long userId,
            Long marketId,
            String clientOrderId
    ) implements OrderCommand {
        @Override
        public CommandType commandType() {
            return CommandType.CANCEL_ORDER;
        }
    }

    @Override
    protected Command normalize(Command command) {
        return command;
    }

    @Override
    protected void validateBusiness(Command command, Market market, User user, String commandId) {
        Order targetOrder = findRequiredOrder(command);
        if (targetOrder.getStatus() != OrderStatus.PENDING && targetOrder.getStatus() != OrderStatus.OPEN) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OPEN);
        }
    }

    @Override
    protected CommandMessage toCommandMessage(Long commandLogId, String commandId, Command command, Market market) {
        Order targetOrder = findRequiredOrder(command);

        return new CommandMessage.Cancel(
                commandLogId,
                command.userId(),
                command.marketId(),
                market.getMarketCode(),
                command.clientOrderId(),
                targetOrder.getOrderKey()
        );
    }

    private Order findRequiredOrder(Command command) {
        return orderRepository
                .findByUserIdAndClientOrderIdAndMarketId(
                        command.userId(),
                        command.clientOrderId(),
                        command.marketId()
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
