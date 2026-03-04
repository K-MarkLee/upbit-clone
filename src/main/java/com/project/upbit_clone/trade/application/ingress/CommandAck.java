package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;

import java.time.LocalDateTime;

public record CommandAck(
        String commandId,
        Long commandLogId,
        CommandType commandType,
        Status status,
        boolean idempotencyHit,
        LocalDateTime acceptedAt
) {


    public static CommandAck accepted(CommandLog commandLog, boolean idempotencyHit) {
        return new CommandAck(
                commandLog.getCommandId(),
                commandLog.getId(),
                commandLog.getCommandType(),
                Status.ACCEPTED,
                idempotencyHit,
                commandLog.getCreatedAt()
        );
    }

    public enum Status {
        ACCEPTED
    }
}
