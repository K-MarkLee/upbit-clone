package com.project.upbit_clone.trade.infrastructure.persistence.model;

import com.project.upbit_clone.global.domain.vo.NonNegativeAmount;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(name = "order_book_projection")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderBookProjection {

    @EmbeddedId
    private OrderBookProjectionId id;

    @Column(name = "total_qty", precision = 30, scale = 8, nullable = false)
    private BigDecimal totalQty;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static OrderBookProjection create(
            // TODO : 저장 검증 필요 즉 worker의 result에만 의존하면안됨.

            OrderBookProjectionId id,
            BigDecimal totalQty,
            Integer orderCount
    ) {
        validateCreateInput(id, totalQty, orderCount);
        return new OrderBookProjection(id, totalQty, orderCount);
    }

    private OrderBookProjection(OrderBookProjectionId id, BigDecimal totalQty, Integer orderCount) {
        this.id = id;
        this.totalQty = new NonNegativeAmount(totalQty).value();
        this.orderCount = validateOrderCount(orderCount);
    }

    public void update(BigDecimal totalQty, Integer orderCount) {
        validateCreateInput(this.id, totalQty, orderCount);
        this.totalQty = new NonNegativeAmount(totalQty).value();
        this.orderCount = validateOrderCount(orderCount);
    }

    private static int validateOrderCount(Integer value) {
        if (value == null || value < 0) {
            throw new BusinessException(ErrorCode.NEGATIVE_ORDER_COUNT_NOT_ALLOWED);
        }
        return value;
    }

    public static void validateCreateInput(OrderBookProjectionId id, BigDecimal totalQty, Integer orderCount) {
        if (id == null || totalQty == null || orderCount == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_BOOK_PROJECTION_INPUT);
        }
    }
}
