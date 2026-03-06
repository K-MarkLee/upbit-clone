package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;

public record CommandAck(
        String commandId,
        Long commandLogId,
        CommandType commandType,
        Status status,
        boolean idempotencyHit
) {


    public static CommandAck accepted(CommandLog commandLog, boolean idempotencyHit) {
        return new CommandAck(
                commandLog.getCommandId(),
                commandLog.getId(),
                commandLog.getCommandType(),
                Status.ACCEPTED,
                idempotencyHit
        );
    }

    public enum Status {
        ACCEPTED
    }
}