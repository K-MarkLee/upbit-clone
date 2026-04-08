package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.application.dispatch.CommandDispatcher;
import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

@Service
public class PlaceOrder extends AbstractOrderIngress<PlaceOrder.Command> {

    private static final int MAX_RESERVATION_ATTEMPTS = 2;

    private final OrderWriteService orderWriteService;

    public PlaceOrder(
            UserRepository userRepository,
            MarketRepository marketRepository,
            JsonMapper jsonMapper,
            IdempotencyHitService idempotencyHitService,
            CommandLogAppendService commandLogAppendService,
            OrderCommandHashService orderCommandHashService,
            CommandDispatcher commandDispatcher,
            OrderWriteService orderWriteService
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
        this.orderWriteService = orderWriteService;
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
    protected Command normalize(Command command) {
        return new Command(
                command.userId(),
                command.marketId(),
                command.clientOrderId(),
                command.orderSide(),
                command.orderType(),
                normalizeTif(command.orderType(), command.timeInForce()),
                normalizeDecimal(command.price()),
                normalizeDecimal(command.quantity()),
                normalizeDecimal(command.quoteAmount())
        );
    }

    @Override
    protected void validateBusiness(Command command, Market market, User user, String commandId) {
        validateOrderShape(command);
        validateSupportedTimeInForce(command.timeInForce());
    }

    @Override
    protected CommandLog persistAccepted(Command command, String requestHash, String commandId, User user, Market market) {
        for (int attempt = 1; true; attempt++) {
            try {
                CommandLog commandLog = createCommandLog(command, requestHash, commandId);
                return orderWriteService.writeAcceptedPlace(new OrderWriteService.AcceptedPlaceCommand(
                        commandLog,
                        user,
                        market,
                        command.clientOrderId(),
                        commandId,
                        command.orderSide(),
                        command.orderType(),
                        command.timeInForce(),
                        command.price(),
                        command.quantity(),
                        command.quoteAmount()
                )).commandLog();
            } catch (OptimisticLockingFailureException exception) {
                if (attempt >= MAX_RESERVATION_ATTEMPTS) {
                    throw exception;
                }
            }
        }
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

    private void validateOrderShape(Command command) {
        if (command.orderType() == null || command.orderSide() == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_INPUT);
        }
        switch (command.orderType()) {
            case LIMIT -> {
                switch (command.orderSide()) {
                    case BID -> validateLimitBidShape(command);
                    case ASK -> validateLimitAskShape(command);
                }
            }
            case MARKET -> {
                switch (command.orderSide()) {
                    case BID -> validateMarketBidShape(command);
                    case ASK -> validateMarketAskShape(command);
                }
            }
            default -> throw new BusinessException(ErrorCode.INVALID_ORDER_INPUT);
        }
    }

    private void validateLimitBidShape(Command command) {
        if (command.price() == null || command.quantity() == null || command.quoteAmount() != null) {
            throw new BusinessException(ErrorCode.INVALID_LIMIT_BID_INPUT);
        }
    }

    private void validateLimitAskShape(Command command) {
        if (command.price() == null || command.quantity() == null || command.quoteAmount() != null) {
            throw new BusinessException(ErrorCode.INVALID_LIMIT_ASK_INPUT);
        }
    }

    private void validateMarketBidShape(Command command) {
        if (command.price() != null || command.quantity() != null || command.quoteAmount() == null) {
            throw new BusinessException(ErrorCode.INVALID_MARKET_BID_INPUT);
        }
    }

    private void validateMarketAskShape(Command command) {
        if (command.price() != null || command.quantity() == null || command.quoteAmount() != null) {
            throw new BusinessException(ErrorCode.INVALID_MARKET_ASK_INPUT);
        }
    }

    private void validateSupportedTimeInForce(TimeInForce timeInForce) {
        if (timeInForce == TimeInForce.FOK) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_TIME_IN_FORCE);
        }
    }

    private TimeInForce normalizeTif(OrderType orderType, TimeInForce timeInForce) {
        if (orderType == OrderType.MARKET && (timeInForce == null || timeInForce == TimeInForce.IOC)) {
            return TimeInForce.IOC;
        }
        if (orderType == OrderType.LIMIT && (timeInForce == null || timeInForce == TimeInForce.GTC)) {
            return TimeInForce.GTC;
        }
        return timeInForce;
    }

    private BigDecimal normalizeDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return normalized;
    }
}
