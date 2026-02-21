package com.project.upbit_clone.trade.domain.model;

import com.project.upbit_clone.global.domain.model.BaseEntity;
import com.project.upbit_clone.global.domain.vo.NonNegativeAmount;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
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
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orders_client", columnNames = {"user_id", "client_order_id"})
        }
)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "time_in_force", nullable = false)
    private TimeInForce timeInForce;

    @Column(name = "price", precision = 30, scale = 8)
    private BigDecimal price;

    @Column(name = "quantity", precision = 30, scale = 8)
    private BigDecimal quantity;

    @Column(name = "quote_amount", precision = 30, scale = 8)
    private BigDecimal quoteAmount;

    @Column(name = "executed_quantity", precision = 30, scale = 8, nullable = false)
    private BigDecimal executedQuantity;

    @Column(name = "executed_quote_amount", precision = 30, scale = 8, nullable = false)
    private BigDecimal executedQuoteAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "cancel_reason", length = 50)
    private String cancelReason;

    public static Order create(CreateCommand command) {
        validateCreateCommand(command);
        validateCreatePolicy(command);
        validateMarketPolicy(command);

        TimeInForce resolvedTif = resolveTimeInForce(command.orderType(), command.timeInForce());

        return new Order(command, resolvedTif);
    }

    private Order(CreateCommand command, TimeInForce resolvedTif) {
        this.market = command.market();
        this.user = command.user();
        this.clientOrderId = command.clientOrderId();
        this.orderSide = command.orderSide();
        this.orderType = command.orderType();
        this.timeInForce = resolvedTif;
        this.price = command.price();
        this.quantity = command.quantity();
        this.quoteAmount = command.quoteAmount();
        this.executedQuantity = new NonNegativeAmount(BigDecimal.ZERO).value();
        this.executedQuoteAmount = new NonNegativeAmount(BigDecimal.ZERO).value();
        this.status = OrderStatus.OPEN;
        this.cancelReason = null;
    }

    public record CreateCommand(
            Market market,
            User user,
            String clientOrderId,
            OrderSide orderSide,
            OrderType orderType,
            TimeInForce timeInForce,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount
    ) {
    }


    /************* 검증, 정책 및 메서드 *************/

    // 주문 정책 검증
    private static void validateCreatePolicy(CreateCommand command) {
        boolean hasPrice = command.price() != null;
        boolean hasQuantity = command.quantity() != null;
        boolean hasQuoteAmount = command.quoteAmount() != null;

        // 지정가 매수 검증
        if (command.orderType() == OrderType.LIMIT && command.orderSide() == OrderSide.BID) {
            validateLimitBidPolicy(command, hasPrice, hasQuantity, hasQuoteAmount);
            return;
        }
        // 지정가 매도 검증
        if (command.orderType() == OrderType.LIMIT && command.orderSide() == OrderSide.ASK) {
            validateLimitAskPolicy(command, hasPrice, hasQuantity, hasQuoteAmount);
            return;
        }
        // 시장가 매수 검증
        if (command.orderType() == OrderType.MARKET && command.orderSide() == OrderSide.BID) {
            validateMarketBidPolicy(command, hasPrice, hasQuantity, hasQuoteAmount);
            return;
        }
        // 시장가 매도 검증
        if (command.orderType() == OrderType.MARKET && command.orderSide() == OrderSide.ASK) {
            validateMarketAskPolicy(command, hasPrice, hasQuantity, hasQuoteAmount);
            return;
        }
        throw new BusinessException(ErrorCode.INVALID_ORDER_INPUT);
    }

    // 거래 정책 검증
    private static void validateMarketPolicy(CreateCommand command) {
        Market market = command.market();
        BigDecimal tickSize = market.getTickSize();
        BigDecimal minOrderQuote = market.getMinOrderQuote();

        /*
         * 지정가 거래시
         * price % tickSize == 0
         * price * quantity >= minOrderQuote
         * */
        if (command.orderType() == OrderType.LIMIT) {
            ErrorCode errorCode = (command.orderSide() == OrderSide.BID)
                    ? ErrorCode.INVALID_LIMIT_BID_INPUT
                    : ErrorCode.INVALID_LIMIT_ASK_INPUT;
            validateTickSize(command.price(), tickSize, errorCode);
            validateMinOrderQuote(command.price().multiply(command.quantity()), minOrderQuote, errorCode);
            return;
        }

        /*
         * 시장가 매수시
         * quoteAmount >= minOrderQuote
         * */
        if (command.orderType() == OrderType.MARKET && command.orderSide() == OrderSide.BID) {
            validateMinOrderQuote(command.quoteAmount(), minOrderQuote, ErrorCode.INVALID_MARKET_BID_INPUT);
        }
    }

    // 주문 조건 검증
    private static TimeInForce resolveTimeInForce(OrderType orderType, TimeInForce timeInForce) {
        // FOK는 미지원
        if (timeInForce == TimeInForce.FOK) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_TIME_IN_FORCE);
        }

        // IOC는 시장 거래시
        if (orderType == OrderType.MARKET) {
            if (timeInForce == null || timeInForce == TimeInForce.IOC) {
                return TimeInForce.IOC;
            }
            throw new BusinessException(ErrorCode.INVALID_ORDER_INPUT);
        }

        // GTC는 디폴트
        if (timeInForce == null || timeInForce == TimeInForce.GTC) {
            return TimeInForce.GTC;
        }
        throw new BusinessException(ErrorCode.INVALID_ORDER_INPUT);
    }

    // null 검증.
    private static void validateCreateCommand(Order.CreateCommand command) {
        if (command == null
                || command.market() == null
                || command.user() == null
                || command.clientOrderId() == null
                || command.clientOrderId().isBlank()
                || command.orderSide() == null
                || command.orderType() == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_INPUT);
        }
    }

    /*
     * 지정가 매수 정책
     * 허용 : price, quantity
     * 금지 : quoteAmount
     * 범위 : price > 0, quantity > 0
     * 예시 : KRW M원에 BTC N개 매수예약
     * */
    private static void validateLimitBidPolicy(
            CreateCommand command, boolean hasPrice, boolean hasQuantity, boolean hasQuoteAmount
    ) {
        if (!hasPrice || !hasQuantity || hasQuoteAmount) {
            throw new BusinessException(ErrorCode.INVALID_LIMIT_BID_INPUT);
        }
        validatePositive(command.price(), ErrorCode.INVALID_LIMIT_BID_INPUT);
        validatePositive(command.quantity(), ErrorCode.INVALID_LIMIT_BID_INPUT);
    }

    /*
     * 지정가 매도 정책
     * 허용 : price, quantity
     * 금지 : quoteAmount
     * 범위 : price > 0, quantity > 0
     * 예시 : BTC N개를 KRW M원에 매도예약
     * */
    private static void validateLimitAskPolicy(
            CreateCommand command, boolean hasPrice, boolean hasQuantity, boolean hasQuoteAmount
    ) {
        if (!hasPrice || !hasQuantity || hasQuoteAmount) {
            throw new BusinessException(ErrorCode.INVALID_LIMIT_ASK_INPUT);
        }
        validatePositive(command.price(), ErrorCode.INVALID_LIMIT_ASK_INPUT);
        validatePositive(command.quantity(), ErrorCode.INVALID_LIMIT_ASK_INPUT);
    }

    /*
     * 시장가 매수 정책
     * 허용 : quoteAmount
     * 금지 : price, quantity
     * 범위 : quoteAmount > 0
     * 예시 : M원에 가능한 만큼 매수
     * */
    private static void validateMarketBidPolicy(
            CreateCommand command, boolean hasPrice, boolean hasQuantity, boolean hasQuoteAmount
    ) {
        if (hasPrice || hasQuantity || !hasQuoteAmount) {
            throw new BusinessException(ErrorCode.INVALID_MARKET_BID_INPUT);
        }
        validatePositive(command.quoteAmount(), ErrorCode.INVALID_MARKET_BID_INPUT);
    }

    /*
     * 시장가 매도 정책
     * 허용 : quantity
     * 금지 : price, quoteAmount
     * 범위 : quantity > 0
     * 예시 : N개를 시장가에 매도
     * */
    private static void validateMarketAskPolicy(
            CreateCommand command, boolean hasPrice, boolean hasQuantity, boolean hasQuoteAmount
    ) {
        if (hasPrice || !hasQuantity || hasQuoteAmount) {
            throw new BusinessException(ErrorCode.INVALID_MARKET_ASK_INPUT);
        }
        validatePositive(command.quantity(), ErrorCode.INVALID_MARKET_ASK_INPUT);
    }

    // 주문 입력값 음수 방지
    private static void validatePositive(BigDecimal amount, ErrorCode errorCode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(errorCode);
        }
    }

    // tick_size 검증. (price를 tick_size로 나눴을떄 나머지가 0이어야함.)
    private static void validateTickSize(BigDecimal price, BigDecimal tickSize, ErrorCode errorCode) {
        // remainder로 나머지를 구하고 compareTo로 비교.
        if (price.remainder(tickSize).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(errorCode);
        }
    }

    // min_order_quote 검증.
    private static void validateMinOrderQuote(BigDecimal value, BigDecimal minOrderQuote, ErrorCode errorCode) {
        //
        if (value.compareTo(minOrderQuote) < 0) {
            throw new BusinessException(errorCode);
        }
    }

    // 체결 수량 적용
    public void applyExecutedQuantity(BigDecimal executedQuantity, BigDecimal executedQuoteAmount) {
        // OPEN 상태 검증
        if (this.status != OrderStatus.OPEN) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OPEN);
        }

        // 입력값 검증 ( null / 1이상 )
        if (executedQuantity == null || executedQuoteAmount == null
                || executedQuantity.compareTo(BigDecimal.ZERO) <= 0
                || executedQuoteAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TRADE_INPUT);
        }
        this.executedQuantity = this.executedQuantity.add(executedQuantity);
        this.executedQuoteAmount = this.executedQuoteAmount.add(executedQuoteAmount);

        // 상태 open -> filled 변경.
        if (isFilledNow()) {
            this.status = OrderStatus.FILLED;
            this.cancelReason = null;
        }
    }

    // 취소 이유 수정
    public void cancel(String reason) {
        if (this.status != OrderStatus.OPEN) {
            return;
        }
        this.status = OrderStatus.CANCELED;
        this.cancelReason = (reason == null || reason.isBlank()) ? "Order Canceled" : reason;
    }

    // 체결 금액 혹은 갯수가 0개 인 경우 검증. (거래 최종 완료)
    private boolean isFilledNow() {
        if (this.orderType == OrderType.MARKET && this.orderSide == OrderSide.BID) {
            return this.executedQuoteAmount.compareTo(this.quoteAmount) >= 0;
        }
        return this.executedQuantity.compareTo(this.quantity) >= 0;
    }

}
