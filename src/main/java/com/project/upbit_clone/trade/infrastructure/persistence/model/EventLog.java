package com.project.upbit_clone.trade.infrastructure.persistence.model;

import com.project.upbit_clone.trade.infrastructure.persistence.vo.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(
        name = "event_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_log_event_id", columnNames = "event_id")
        },
        indexes = {
                @Index(name = "idx_event_log_market_seq", columnList = "market_id, event_log_id"),
                @Index(name = "idx_event_log_command", columnList = "command_log_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "command_log_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_event_log_command_log")
    )
    private CommandLog commandLog;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public static EventLog create(CreateCommand command) {
        return new EventLog(command);
    }

    private EventLog(CreateCommand command) {
        this.commandLog = Objects.requireNonNull(command.commandLog(), "commandLog");
        this.eventId = requireText(command.eventId(), "eventId");
        this.eventType = Objects.requireNonNull(command.eventType(), "eventType");
        this.marketId = Objects.requireNonNull(command.marketId(), "marketId");
        this.orderId = command.orderId();
        this.payload = requireText(command.payload(), "payload");
    }

    public record CreateCommand(
            CommandLog commandLog,
            String eventId,
            EventType eventType,
            Long marketId,
            Long orderId,
            String payload
    ) {
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
