package com.project.upbit_clone.trade.presentation.response;

import com.project.upbit_clone.trade.application.ingress.CommandAck;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderResponse {

    private final String commandId;
    private final Long commandLogId;
    private final String commandType;
    private final String  status;
    private final boolean idempotencyHit;
    private final LocalDateTime acceptedAt;

    public static OrderResponse from(CommandAck ack) {
        return OrderResponse.builder()
                .commandId(ack.commandId())
                .commandLogId(ack.commandLogId())
                .commandType(ack.commandType().name())
                .status(ack.status().name())
                .idempotencyHit(ack.idempotencyHit())
                .acceptedAt(ack.acceptedAt())
                .build();
    }
}
