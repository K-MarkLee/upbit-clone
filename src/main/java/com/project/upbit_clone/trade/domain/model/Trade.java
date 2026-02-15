package com.project.upbit_clone.trade.domain.model;

import com.project.upbit_clone.global.domain.vo.NonNegativeAmount;
import com.project.upbit_clone.global.domain.vo.PositiveAmount;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
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
    private BigDecimal feeRate;

    @Column(name = "buy_fee_amount", precision = 30, scale = 8, nullable = false)
    private BigDecimal buyFeeAmount;

    @Column(name = "sell_fee_amount", precision = 30, scale = 8, nullable = false)
    private BigDecimal sellFeeAmount;

    @Column(name = "executed_at", insertable = false, updatable = false, nullable = false)
    private LocalDateTime executedAt;

    public static Trade create(CreateCommand command) {
        if (command.buyOrder().getUser().getId().equals(command.sellOrder().getUser().getId())) {
            throw new BusinessException(ErrorCode.SELF_TRADE_NOT_ALLOWED);
        }
        return new Trade(command);
    }

    private Trade(CreateCommand command) {
        this.market = command.market();
        this.buyOrder = command.buyOrder();
        this.sellOrder = command.sellOrder();
        this.isBuyerMaker = command.isBuyerMaker();
        this.price = new PositiveAmount(command.price()).value();
        this.quantity = new PositiveAmount(command.quantity()).value();
        this.quoteAmount = new PositiveAmount(command.quoteAmount()).value();
        this.feeRate = new NonNegativeAmount(command.feeRate() == null ? BigDecimal.ZERO : command.feeRate()).value();
        this.buyFeeAmount = new NonNegativeAmount(
                command.buyFeeAmount() == null ? BigDecimal.ZERO : command.buyFeeAmount()).value();
        this.sellFeeAmount = new NonNegativeAmount(
                command.sellFeeAmount() == null ? BigDecimal.ZERO : command.sellFeeAmount()).value();
    }

    public record CreateCommand(
            Market market,
            Order buyOrder,
            Order sellOrder,
            Boolean isBuyerMaker,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount,
            BigDecimal feeRate,
            BigDecimal buyFeeAmount,
            BigDecimal sellFeeAmount
    ) {
    }
}
