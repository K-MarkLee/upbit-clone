package com.project.upbit_clone.trade.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "trade")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_order_id", nullable = false)
    private Order buyOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sell_order_id", nullable = false)
    private Order sellOrder;

    @Column(name = "is_buyer_maker", nullable = false)
    private Boolean isBuyerMaker;

    @Column(name = "price", precision = 30, scale = 8, nullable = false)
    private BigDecimal price;

    @Column(name = "quantity", precision = 30, scale = 8, nullable = false)
    private BigDecimal quantity;

    @Column(name = "quote_amount", precision = 30, scale = 8, nullable = false)
    private BigDecimal quoteAmount;

    @Column(name = "fee_rate", precision = 10, scale = 8, nullable = false)
    private BigDecimal feeRate = BigDecimal.ZERO;

    @Column(name = "buy_fee_amount", precision = 30, scale = 8, nullable = false)
    private BigDecimal buyFeeAmount = BigDecimal.ZERO;

    @Column(name = "sell_fee_amount", precision = 30, scale = 8, nullable = false)
    private BigDecimal sellFeeAmount = BigDecimal.ZERO;

    @Column(name = "executed_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime executedAt;
}
