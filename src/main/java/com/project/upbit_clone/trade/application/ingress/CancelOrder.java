package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CancelOrder extends AbstractOrderIngress<CancelOrder.Command> {

    private final OrderRepository orderRepository;

    public CancelOrder(
            CommandLogRepository commandLogRepository,
            UserRepository userRepository,
            MarketRepository marketRepository,
            OrderRepository orderRepository,
            JsonMapper jsonMapper
    ) {
        super(commandLogRepository, userRepository, marketRepository, jsonMapper);
        this.orderRepository = orderRepository;
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
        Order order = orderRepository
                .findByUser_IdAndMarket_IdAndClientOrderId(
                        command.userId(),
                        command.marketId(),
                        command.clientOrderId()
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OPEN);
        }
    }
}
