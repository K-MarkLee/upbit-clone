package com.project.upbit_clone.trade.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(name = "order_book_projection")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderBookProjection {

    @EmbeddedId
    private OrderBookProjectionId id;

    @Column(name = "total_qty", precision = 30, scale = 8, nullable = false)
    private BigDecimal totalQty;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @Column(name = "updated_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime updatedAt;

    public static OrderBookProjection create(
            OrderBookProjectionId id,
            BigDecimal totalQty,
            Integer orderCount
    ) {
        return new OrderBookProjection(id, totalQty, orderCount);
    }

    private OrderBookProjection(OrderBookProjectionId id, BigDecimal totalQty, Integer orderCount) {
        this.id = Objects.requireNonNull(id, "id");
        this.totalQty = normalizeQty(totalQty);
        this.orderCount = normalizeOrderCount(orderCount);
    }

    public void updateLevel(BigDecimal totalQty, Integer orderCount) {
        this.totalQty = normalizeQty(totalQty);
        this.orderCount = normalizeOrderCount(orderCount);
    }

    private static BigDecimal normalizeQty(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalQty must not be negative");
        }
        return value;
    }

    private static int normalizeOrderCount(Integer value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("orderCount must not be negative");
        }
        return value;
    }
}
