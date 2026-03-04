package com.project.upbit_clone.trade.application.ingress;

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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

@Service
public class CancelOrder extends AbstractOrderIngress<CancelOrder.Command> {

    private final CommandLogRepository commandLogRepository;

    public CancelOrder(
            CommandLogRepository commandLogRepository,
            UserRepository userRepository,
            MarketRepository marketRepository,
            JsonMapper jsonMapper
    ) {
        super(commandLogRepository, userRepository, marketRepository, jsonMapper);
        this.commandLogRepository = commandLogRepository;
    }

    @Transactional
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
    protected void validateBusiness(Command command, Market market, User user) {
        Optional<CommandLog> placeCommand = commandLogRepository
                .findByUserIdAndClientOrderIdAndCommandType(
                        command.userId(),
                        command.clientOrderId(),
                        CommandType.PLACE_ORDER
                );
        if (placeCommand.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!command.marketId().equals(placeCommand.get().getMarketId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
    }
}
