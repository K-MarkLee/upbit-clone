package com.project.upbit_clone.trade.domain.model;

import com.project.upbit_clone.global.domain.vo.NonNegativeAmount;
import com.project.upbit_clone.global.domain.vo.PositiveAmount;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Table(name = "trade")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.0005");

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

    @Enumerated(EnumType.STRING)
    @Column(name = "maker_order_side", nullable = false)
    private OrderSide makerOrderSide;

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
        validateCreateCommand(command);

        // buyOrder와 sellOrder가 각 BID와 ASK가 맞는지 검증.
        if (command.buyOrder().getOrderSide() != OrderSide.BID || command.sellOrder().getOrderSide() != OrderSide.ASK) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_SIDE);
        }

        // 거래 시 구매자의 아이디와 판매자의 아이디가 다른지 검증.
        if (command.buyOrder().getUser().getId().equals(command.sellOrder().getUser().getId())) {
            throw new BusinessException(ErrorCode.SELF_TRADE_NOT_ALLOWED);
        }

        // 체결의 market과 양쪽 주문의 market은 모두 같은지 검증.
        if (isDifferentMarket(command.market(), command.buyOrder().getMarket())
                || isDifferentMarket(command.market(), command.sellOrder().getMarket())) {
            throw new BusinessException(ErrorCode.TRADE_MARKET_MISMATCH);
        }

        // 체결가가 maker 의 가격이 맞는지 검증.
        BigDecimal makerPrice = (command.makerOrderSide() == OrderSide.BID)
                ? command.buyOrder().getPrice()
                : command.sellOrder().getPrice();
        if (makerPrice == null || command.price().compareTo(makerPrice) != 0) {
            throw new BusinessException(ErrorCode.TRADE_PRICE_MUST_BE_MAKER_PRICE);
        }

        return new Trade(command);
    }

    // left 와 right 마켓이 같으면 false, 맞으면 true 반환.
    private static boolean isDifferentMarket(Market left, Market right) {
        if (left == null || right == null) {
            return true;
        }

        if (left.getId() != null && right.getId() != null) {
            return !Objects.equals(left.getId(), right.getId());
        }
        return left != right;
    }

    private Trade(CreateCommand command) {
        this.market = command.market();
        this.buyOrder = command.buyOrder();
        this.sellOrder = command.sellOrder();
        this.makerOrderSide = command.makerOrderSide;
        this.price = new PositiveAmount(command.price()).value();
        this.quantity = new PositiveAmount(command.quantity()).value();
        this.quoteAmount = new PositiveAmount(command.quoteAmount()).value();
        this.feeRate = new NonNegativeAmount(command.feeRate() == null ? DEFAULT_FEE_RATE : command.feeRate()).value();
        this.buyFeeAmount = new NonNegativeAmount(
                command.buyFeeAmount() == null ? BigDecimal.ZERO : command.buyFeeAmount()).value();
        this.sellFeeAmount = new NonNegativeAmount(
                command.sellFeeAmount() == null ? BigDecimal.ZERO : command.sellFeeAmount()).value();
    }

    public record CreateCommand(
            Market market,
            Order buyOrder,
            Order sellOrder,
            OrderSide makerOrderSide,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount,
            BigDecimal feeRate,
            BigDecimal buyFeeAmount,
            BigDecimal sellFeeAmount
    ) {
    }

    // 생성자 null 검증.
    private static void validateCreateCommand(CreateCommand command) {
        if (command == null
                || command.market() == null
                || command.buyOrder() == null
                || command.sellOrder() == null
                || command.makerOrderSide() == null
                || command.price() == null
                || command.quantity() == null
                || command.quoteAmount() == null) {
            throw new BusinessException(ErrorCode.INVALID_TRADE_INPUT);
        }
    }

}
