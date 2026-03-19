package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.application.dispatch.CommandDispatcher;
import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

abstract class AbstractOrderIngress<C extends OrderCommand> {

    private static final Logger log = LoggerFactory.getLogger(AbstractOrderIngress.class);

    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final JsonMapper jsonMapper;
    private final IdempotencyHitService idempotencyHitService;
    private final CommandLogAppendService commandLogAppendService;
    private final OrderCommandHashService orderCommandHashService;
    private final CommandDispatcher commandDispatcher;

    protected AbstractOrderIngress(
            UserRepository userRepository,
            MarketRepository marketRepository,
            JsonMapper jsonMapper,
            IdempotencyHitService idempotencyHitService,
            CommandLogAppendService commandLogAppendService,
            OrderCommandHashService orderCommandHashService,
            CommandDispatcher commandDispatcher
    ) {
        this.userRepository = userRepository;
        this.marketRepository = marketRepository;
        this.jsonMapper = jsonMapper;
        this.idempotencyHitService = idempotencyHitService;
        this.commandLogAppendService = commandLogAppendService;
        this.orderCommandHashService = orderCommandHashService;
        this.commandDispatcher = commandDispatcher;
    }

    protected CommandAck handleInternal(C command) {
        validateInput(command);
        String requestHash = orderCommandHashService.hash(command);

        Optional<CommandLog> hit = idempotencyHitService.find(
                command.userId(),
                command.clientOrderId(),
                command.commandType()
        );
        if (hit.isPresent()) {
            return resolveIdempotencyHit(hit.get(), requestHash);
        }

        User user = validateAndGetUser(command.userId());
        Market market = validateAndGetMarket(command.marketId());
        validateBusiness(command, market, user);

        CommandLog commandLog = createCommandLog(command, requestHash);
        CommandLog saved;
        try {
            saved = commandLogAppendService.append(commandLog);

        } catch (DataIntegrityViolationException exception) {
            Optional<CommandLog> recovered = idempotencyHitService.findInNewTransaction(
                    command.userId(),
                    command.clientOrderId(),
                    command.commandType()
            );
            if (recovered.isEmpty()) {
                throw exception;
            }
            return resolveIdempotencyHit(recovered.get(), requestHash);
        }
        try {
            commandDispatcher.dispatch(toCommandMessage(saved.getId(), command));
        } catch (RuntimeException exception) {
            // dispatch 실패 로거
            log.error(
                    "Append 후 command dispatch를 실패했습니다. commandLogId={}, commandType={}, userId={}, marketId={}, clientOrderId={}",
                    saved.getId(),
                    command.commandType(),
                    command.userId(),
                    command.marketId(),
                    command.clientOrderId(),
                    exception
            );
        }
        return CommandAck.accepted(saved, false);
    }

    protected void validateBusiness(C command, Market market, User user) {
    }

    protected abstract CommandMessage toCommandMessage(Long commandLogId, C command);

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
        Market market = marketRepository.findWithAssetsById(marketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MARKET_NOT_FOUND));
        if (market.getStatus() != EnumStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_ALLOWED_MARKET_STATUS);
        }
        return market;
    }

    // 커맨드 로그 생성 (주문 생성 : CREATE, CANCEL)
    private CommandLog createCommandLog(OrderCommand command, String requestHash) {
        String commandId = UUID.randomUUID().toString();
        return CommandLog.create(new CommandLog.CreateCommand(
                commandId,
                command.commandType(),
                command.marketId(),
                command.userId(),
                command.clientOrderId(),
                toPayload(command),
                requestHash
        ));
    }

    // 멱등성 히트
    private CommandAck resolveIdempotencyHit(CommandLog commandLog, String requestHash) {
        if (!requestHash.equals(commandLog.getRequestHash())) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
        return CommandAck.accepted(commandLog, true);
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
