package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IdempotencyHitService {

    private final CommandLogRepository commandLogRepository;

    public IdempotencyHitService(CommandLogRepository commandLogRepository) {
        this.commandLogRepository = commandLogRepository;
    }

    @Transactional(readOnly = true)
    public Optional<CommandLog> find(Long userId, String clientOrderId, CommandType commandType) {
        return commandLogRepository
                .findByUserIdAndClientOrderIdAndCommandType(userId, clientOrderId, commandType);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<CommandLog> findInNewTransaction(Long userId, String clientOrderId, CommandType commandType) {
        return commandLogRepository
                .findByUserIdAndClientOrderIdAndCommandType(userId, clientOrderId, commandType);
    }
}
