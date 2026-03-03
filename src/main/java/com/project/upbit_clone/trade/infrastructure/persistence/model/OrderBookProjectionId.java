package com.project.upbit_clone.trade.infrastructure.persistence.model;

import com.project.upbit_clone.trade.domain.vo.OrderSide;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
// order book projection 복합키
public class OrderBookProjectionId implements Serializable {

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 3)
    private OrderSide side;

    @Column(name = "price", precision = 30, scale = 8, nullable = false)
    private BigDecimal price;
}
