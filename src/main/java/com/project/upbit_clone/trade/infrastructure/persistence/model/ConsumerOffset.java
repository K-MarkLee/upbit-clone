package com.project.upbit_clone.trade.infrastructure.persistence.model;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
        // TODO : 저장 검증 필요 즉 worker의 result에만 의존하면안됨.

        validateCreateInput(id, lastOffset);
        return new ConsumerOffset(id, lastOffset);
    }

    private ConsumerOffset(ConsumerOffsetId id, Long lastOffset) {
        this.id = id;
        this.lastOffset = validateOffset(lastOffset);
    }

    public static void validateCreateInput(ConsumerOffsetId id, Long lastOffset) {
        if (id == null || lastOffset == null) {
            throw new BusinessException(ErrorCode.INVALID_CONSUMER_OFFSET_INPUT);
        }
    }

    private Long validateOffset(Long offset) {
        if (offset < 0) {
            throw new BusinessException(ErrorCode.NEGATIVE_OFFSET_NOT_ALLOWED);
        }
        return offset;
    }

}
