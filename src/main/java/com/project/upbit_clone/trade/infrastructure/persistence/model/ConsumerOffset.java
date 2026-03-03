package com.project.upbit_clone.trade.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(name = "consumer_offset")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumerOffset {

    @EmbeddedId
    private ConsumerOffsetId id;

    @Column(name = "last_offset", nullable = false)
    private Long lastOffset;

    @Column(name = "updated_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime updatedAt;

    public static ConsumerOffset create(ConsumerOffsetId id, Long lastOffset) {
        return new ConsumerOffset(id, lastOffset);
    }

    private ConsumerOffset(ConsumerOffsetId id, Long lastOffset) {
        this.id = Objects.requireNonNull(id, "id");
        this.lastOffset = normalizeNonNegative(lastOffset);
    }

    public void updateLastOffset(Long lastOffset) {
        this.lastOffset = normalizeNonNegative(lastOffset);
    }

    private static long normalizeNonNegative(Long value) {
        if (value == null) {
            return 0L;
        }
        if (value < 0L) {
            throw new IllegalArgumentException("lastOffset must not be negative");
        }
        return value;
    }
}
