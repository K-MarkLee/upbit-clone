package com.project.upbit_clone.trade.infrastructure.persistence.model;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "command_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_command_log_command_id", columnNames = "command_id"),
                @UniqueConstraint(
                        name = "uk_command_log_user_client_type",
                        columnNames = {"user_id", "client_order_id", "command_type"}
                )
        },
        indexes = {
                @Index(name = "idx_command_log_market_seq", columnList = "market_id, command_log_id"),
                @Index(name = "idx_command_log_user_client", columnList = "user_id, client_order_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommandLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "command_log_id")
    private Long id;

    @Column(name = "command_id", nullable = false, length = 64)
    private String commandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 32)
    private CommandType commandType;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "client_order_id", length = 150)
    private String clientOrderId;

    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public static CommandLog create(CreateCommand command) {
        validateCreateCommand(command);

        return new CommandLog(command);
    }

    private CommandLog(CreateCommand command) {
        this.commandId = command.commandId();
        this.commandType = command.commandType();
        this.marketId = command.marketId();
        this.userId = command.userId();
        this.clientOrderId = command.clientOrderId();
        this.payload = command.payload();
        this.requestHash = command.requestHash();
    }

    public record CreateCommand(
            String commandId,
            CommandType commandType,
            Long marketId,
            Long userId,
            String clientOrderId,
            String payload,
            String requestHash
    ) {
    }

    public static void validateCreateCommand(CreateCommand command) {
        if (command == null
                || command.commandId() == null
                || command.commandId().isBlank()
                || command.commandType() == null
                || command.marketId() == null
                || command.payload() == null
                || command.payload().isBlank()
                || command.requestHash() == null
                || command.requestHash().isBlank()
            ) {
            throw new BusinessException(ErrorCode.INVALID_COMMAND_LOG_INPUT);
        }
    }
}
