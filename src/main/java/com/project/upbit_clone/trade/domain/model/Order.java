package com.project.upbit_clone.trade.domain.model;

import com.project.upbit_clone.global.domain.model.BaseEntity;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.user.domain.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "client_order_id", nullable = false, length = 150)
    private String clientOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_side", nullable = false)
    private OrderSide orderSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    // TODO : 디폴트 값 'GTC' FOK는 구현 안할 예정.
    @Enumerated(EnumType.STRING)
    @Column(name = "time_in_force", nullable = false)
    private TimeInForce timeInForce = TimeInForce.GTC;

    @Column(name = "price", precision = 30, scale = 8)
    private BigDecimal price;

    @Column(name = "quantity", precision = 30, scale = 8)
    private BigDecimal quantity;

    @Column(name = "quote_amount", precision = 30, scale = 8)
    private BigDecimal quoteAmount;

    @Column(name = "executed_quantity", precision = 30, scale = 8, nullable = false)
    private BigDecimal executedQuantity = BigDecimal.ZERO;

    @Column(name = "executed_quote_amount", precision = 30, scale = 8, nullable = false)
    private BigDecimal executedQuoteAmount = BigDecimal.ZERO;

    // TODO : 디폴트 값 'OPEN'
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "cancel_reason", length = 50)
    private String cancelReason;
}
