package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.application.dispatch.CommandDispatcher;
import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CancelOrder extends AbstractOrderIngress<CancelOrder.Command> {

    private final CommandLogRepository commandLogRepository;

    public CancelOrder(
            CommandLogRepository commandLogRepository,
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
        this.commandLogRepository = commandLogRepository;
    }

    public CommandAck handle(Command command) {
        return handleInternal(command);
    }

    public record Command(
            Long userId,
            Long marketId,
            String clientOrderId,
            String cancelReason
    ) implements OrderCommand {
        @Override
        public CommandType commandType() {
            return CommandType.CANCEL_ORDER;
        }
    }

    @Override
    protected void validateBusiness(Command command, Market market, User user, String commandId) {
        CommandLog placeCommand = findRequiredPlaceCommand(command);
        if (!command.marketId().equals(placeCommand.getMarketId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
    }

    @Override
    protected CommandMessage toCommandMessage(Long commandLogId, String commandId, Command command, String marketCode) {
        CommandLog targetPlaceCommand = findRequiredPlaceCommand(command);

        return new CommandMessage.Cancel(
                commandLogId,
                command.userId(),
                command.marketId(),
                marketCode,
                command.clientOrderId(),
                targetPlaceCommand.getCommandId(),
                command.cancelReason()
        );
    }

    private CommandLog findRequiredPlaceCommand(Command command) {
        return commandLogRepository
                .findByUserIdAndClientOrderIdAndCommandType(
                        command.userId(),
                        command.clientOrderId(),
                        CommandType.PLACE_ORDER
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
