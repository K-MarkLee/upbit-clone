package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

abstract class AbstractOrderIngress<C extends OrderCommand> {

    private final CommandLogRepository commandLogRepository;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final JsonMapper jsonMapper;

    protected AbstractOrderIngress(
            CommandLogRepository commandLogRepository,
            UserRepository userRepository,
            MarketRepository marketRepository,
            JsonMapper jsonMapper
    ) {
        this.commandLogRepository = commandLogRepository;
        this.userRepository = userRepository;
        this.marketRepository = marketRepository;
        this.jsonMapper = jsonMapper;
    }

    protected CommandAck handleInternal(C command) {
        validateInput(command);

        Optional<CommandLog> hit = findIdempotencyHit(command.userId(), command.clientOrderId(), command.commandType());
        if (hit.isPresent()) {
            return CommandAck.accepted(hit.get(), true);
        }

        User user = validateAndGetUser(command.userId());
        Market market = validateAndGetMarket(command.marketId());
        validateBusiness(command, market, user);

        CommandLog commandLog = createCommandLog(command);
        try {
            CommandLog saved = commandLogRepository.save(commandLog);
            return CommandAck.accepted(saved, false);
        } catch (DataIntegrityViolationException exception) {
            Optional<CommandLog> recovered = findIdempotencyHit(
                    command.userId(),
                    command.clientOrderId(),
                    command.commandType()
            );
            if (recovered.isEmpty()) {
                throw exception;
            }
            return CommandAck.accepted(recovered.get(), true);
        }
    }

    protected void validateBusiness(C command, Market market, User user) {
    }

    // 최소 검증
    private void validateInput(OrderCommand command) {
        if (command == null
                || command.userId() == null
                || command.marketId() == null
                || command.clientOrderId() == null
                || command.clientOrderId().isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_ORDER_REQUIRED_VALUE);
        }
    }

    // 멱등성 히트
    private Optional<CommandLog> findIdempotencyHit(Long userId, String clientOrderId, CommandType commandType) {
        return commandLogRepository
                .findByUserIdAndClientOrderIdAndCommandType(userId, clientOrderId, commandType);
    }

    // 사용자 검증
    private User validateAndGetUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != EnumStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_ALLOWED_USER_STATUS);
        }
        return user;
    }

    // 시장 검증
    private Market validateAndGetMarket(Long marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MARKET_NOT_FOUND));
        if (market.getStatus() != EnumStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_ALLOWED_MARKET_STATUS);
        }
        return market;
    }

    // 커맨드 로그 생성 (주문 생성 : CREATE, CANCEL)
    private CommandLog createCommandLog(OrderCommand command) {
        String commandId = UUID.randomUUID().toString();
        return CommandLog.create(new CommandLog.CreateCommand(
                commandId,
                command.commandType(),
                command.marketId(),
                command.userId(),
                command.clientOrderId(),
                toPayload(command)
        ));
    }

    // 원본 스냅샷
    private String toPayload(OrderCommand command) {
        try {
            return jsonMapper.writeValueAsString(command);
        } catch (JacksonException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "JSON 직렬화에 실패했씁니다."
            );
        }
    }
}
