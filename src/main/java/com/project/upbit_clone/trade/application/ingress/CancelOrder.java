package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CancelOrder extends AbstractOrderIngress<CancelOrder.Command> {

    public CancelOrder(
            CommandLogRepository commandLogRepository,
            UserRepository userRepository,
            MarketRepository marketRepository,
            JsonMapper jsonMapper
    ) {
        super(commandLogRepository, userRepository, marketRepository, jsonMapper);
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
}
