package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommandLogAppendService {

    private final CommandLogRepository commandLogRepository;

    public CommandLogAppendService(CommandLogRepository commandLogRepository) {
        this.commandLogRepository = commandLogRepository;
    }

    // append 트랜잭션 분리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommandLog append(CommandLog commandLog) {
        return commandLogRepository.saveAndFlush(commandLog);
    }
}
