package com.project.upbit_clone.trade.domain.model;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order 도메인 테스트")
class OrderTest {
    private Market market;
    private User user;
    private String clientOrderId;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal quoteAmount;

    @BeforeEach
    void setUp() {
        market = fixtureMarket();
        user = fixtureUser();
        clientOrderId = "client-order-1";
        price = new BigDecimal("10000");
        quantity = BigDecimal.ONE;
        quoteAmount = new BigDecimal("10000");
    }

    @Test
    @DisplayName("Happy : 유효한 LIMIT-BID 입력값으로 생성하면 주문이 생성된다.")
    void create_limit_bid_order_with_valid_inputs() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.LIMIT,
                null, price, quantity, null
        );

        // when
        Order order = Order.create(command);

        // then
        assertThat(order).isNotNull();
        assertThat(order.getMarket()).isEqualTo(market);
        assertThat(order.getUser()).isEqualTo(user);
        assertThat(order.getClientOrderId()).isEqualTo(clientOrderId);
        assertThat(order.getOrderSide()).isEqualTo(OrderSide.BID);
        assertThat(order.getOrderType()).isEqualTo(OrderType.LIMIT);
        assertThat(order.getTimeInForce()).isEqualTo(TimeInForce.GTC);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getQuantity()).isEqualTo(quantity);
        assertThat(order.getQuoteAmount()).isNull();
        assertThat(order.getExecutedQuantity()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getExecutedQuoteAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(order.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("Happy : 유효한 LIMIT-ASK 입력값으로 생성하면 주문이 생성된다.")
    void create_limit_ask_order_with_valid_inputs() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.LIMIT,
                null, price, quantity, null
        );

        // when
        Order order = Order.create(command);

        // then
        assertThat(order).isNotNull();
        assertThat(order.getMarket()).isEqualTo(market);
        assertThat(order.getUser()).isEqualTo(user);
        assertThat(order.getClientOrderId()).isEqualTo(clientOrderId);
        assertThat(order.getOrderSide()).isEqualTo(OrderSide.ASK);
        assertThat(order.getOrderType()).isEqualTo(OrderType.LIMIT);
        assertThat(order.getTimeInForce()).isEqualTo(TimeInForce.GTC);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getQuantity()).isEqualTo(quantity);
        assertThat(order.getQuoteAmount()).isNull();
        assertThat(order.getExecutedQuantity()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getExecutedQuoteAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(order.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("Happy : 유효한 MARKET-BID 입력값으로 생성하면 주문이 생성된다.")
    void create_market_bid_order_with_valid_inputs() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.MARKET,
                TimeInForce.IOC, null, null, quoteAmount
        );

        // when
        Order order = Order.create(command);

        // then
        assertThat(order).isNotNull();
        assertThat(order.getMarket()).isEqualTo(market);
        assertThat(order.getUser()).isEqualTo(user);
        assertThat(order.getClientOrderId()).isEqualTo(clientOrderId);
        assertThat(order.getOrderSide()).isEqualTo(OrderSide.BID);
        assertThat(order.getOrderType()).isEqualTo(OrderType.MARKET);
        assertThat(order.getTimeInForce()).isEqualTo(TimeInForce.IOC);
        assertThat(order.getPrice()).isNull();
        assertThat(order.getQuantity()).isNull();
        assertThat(order.getQuoteAmount()).isEqualTo(quoteAmount);
        assertThat(order.getExecutedQuantity()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getExecutedQuoteAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(order.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("Happy : 유효한 MARKET-ASK 입력값으로 생성하면 주문이 생성된다.")
    void create_market_ask_order_with_valid_inputs() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.MARKET,
                TimeInForce.IOC, null, quantity, null
        );

        // when
        Order order = Order.create(command);

        // then
        assertThat(order).isNotNull();
        assertThat(order.getMarket()).isEqualTo(market);
        assertThat(order.getUser()).isEqualTo(user);
        assertThat(order.getClientOrderId()).isEqualTo(clientOrderId);
        assertThat(order.getOrderSide()).isEqualTo(OrderSide.ASK);
        assertThat(order.getOrderType()).isEqualTo(OrderType.MARKET);
        assertThat(order.getTimeInForce()).isEqualTo(TimeInForce.IOC);
        assertThat(order.getPrice()).isNull();
        assertThat(order.getQuantity()).isEqualTo(quantity);
        assertThat(order.getQuoteAmount()).isNull();
        assertThat(order.getExecutedQuantity()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getExecutedQuoteAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(order.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("Happy : MARKET-BID 주문을 timeInForce null로 생성하면 IOC로 정규화된다.")
    void create_market_bid_order_with_null_tif() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.MARKET,
                null, null, null, quoteAmount
        );

        // when
        Order order = Order.create(command);

        // then
        assertThat(order).isNotNull();
        assertThat(order.getOrderSide()).isEqualTo(OrderSide.BID);
        assertThat(order.getOrderType()).isEqualTo(OrderType.MARKET);
        assertThat(order.getTimeInForce()).isEqualTo(TimeInForce.IOC);
        assertThat(order.getPrice()).isNull();
        assertThat(order.getQuantity()).isNull();
        assertThat(order.getQuoteAmount()).isEqualTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("Happy : MARKET-ASK 주문을 timeInForce null로 생성하면 IOC로 정규화된다.")
    void create_market_ask_order_with_null_tif() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.MARKET,
                null, null, quantity, null
        );

        // when
        Order order = Order.create(command);

        // then
        assertThat(order).isNotNull();
        assertThat(order.getOrderSide()).isEqualTo(OrderSide.ASK);
        assertThat(order.getOrderType()).isEqualTo(OrderType.MARKET);
        assertThat(order.getTimeInForce()).isEqualTo(TimeInForce.IOC);
        assertThat(order.getPrice()).isNull();
        assertThat(order.getQuantity()).isEqualTo(new BigDecimal("1"));
        assertThat(order.getQuoteAmount()).isNull();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullRequiredOrderCommands")
    @DisplayName("Negative : 필수 입력값이 null이면 BusinessException을 반환한다.")
    void create_order_with_null_required_inputs(String caseName, Order.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_INPUT);
    }

    private static Stream<Arguments> nullRequiredOrderCommands() {
        Market market = fixtureMarket();
        User user = fixtureUser();

        return Stream.of(

                Arguments.of("command null", null),
                Arguments.of("market null", createCommand(
                        null, user, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("10000"), BigDecimal.ONE, null
                )),
                Arguments.of("user null", createCommand(
                        market, null, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("10000"), BigDecimal.ONE, null
                )),
                Arguments.of("clientOrderId null", createCommand(
                        market, user, null, OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("10000"), BigDecimal.ONE, null
                )),
                Arguments.of("orderSide null", createCommand(
                        market, user, "client-order-1", null, OrderType.LIMIT, null, new BigDecimal("10000"), BigDecimal.ONE, null
                )),
                Arguments.of("orderType null", createCommand(
                        market, user, "client-order-1", OrderSide.BID, null, null, new BigDecimal("10000"), BigDecimal.ONE, null
                ))
        );
    }

    @Test
    @DisplayName("Negative : client_order_id가 blank면 BusinessException을 반환한다.")
    void create_order_with_blank_client_order_id() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, "", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("10000"), BigDecimal.ONE, null
        );

        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_INPUT);
    }

    @Test
    @DisplayName("Negative : client_order_id가 blank면 BusinessException을 반환한다.")
    void create_order_with_blank_client_order_id_2() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, "   ", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("10000"), BigDecimal.ONE, null
        );

        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_INPUT);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidLimitBidPolicyCommands")
    @DisplayName("Negative : LIMIT-BID 입력 정책을 위반하면 BusinessException을 반환한다.")
    void create_limit_bid_order_with_invalid_policy_inputs(String caseName, Order.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LIMIT_BID_INPUT);
    }

    private static Stream<Arguments> invalidLimitBidPolicyCommands() {
        Market market = fixtureMarket();
        User user = fixtureUser();

        return Stream.of(
                Arguments.of("price null", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, null, BigDecimal.ONE, null
                )),
                Arguments.of("quantity null", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("10000"), null, null
                )),
                Arguments.of("quoteAmount provided", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("10000"), BigDecimal.ONE, new BigDecimal("10000")
                )),
                Arguments.of("price is zero", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, BigDecimal.ZERO, BigDecimal.ONE, null
                )),
                Arguments.of("quantity is zero", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("10000"), BigDecimal.ZERO, null
                )),
                Arguments.of("price is negative", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("-1"), BigDecimal.ONE, null
                )),
                Arguments.of("quantity is negative", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("10000"), new BigDecimal("-1"), null
                ))
        );
    }


    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidLimitAskPolicyCommands")
    @DisplayName("Negative : LIMIT-ASK 입력 정책을 위반하면 BusinessException을 반환한다.")
    void create_limit_ask_order_with_invalid_policy_inputs(String caseName, Order.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LIMIT_ASK_INPUT);
    }

    private static Stream<Arguments> invalidLimitAskPolicyCommands() {
        Market market = fixtureMarket();
        User user = fixtureUser();

        return Stream.of(
                Arguments.of("price null", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.LIMIT, null, null, BigDecimal.ONE, null
                )),
                Arguments.of("quantity null", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.LIMIT, null, new BigDecimal("10000"), null, null
                )),
                Arguments.of("quoteAmount provided", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.LIMIT, null, new BigDecimal("10000"), BigDecimal.ONE, new BigDecimal("10000")
                )),
                Arguments.of("price is zero", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.LIMIT, null, BigDecimal.ZERO, BigDecimal.ONE, null
                )),
                Arguments.of("quantity is zero", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.LIMIT, null, new BigDecimal("10000"), BigDecimal.ZERO, null
                )),
                Arguments.of("price is negative", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.LIMIT, null, BigDecimal.ZERO, new BigDecimal("1"), null
                )),
                Arguments.of("quantity is negative", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.LIMIT, null, new BigDecimal("10000"), new BigDecimal("-1"), null
                ))

        );
    }


    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidMarketBidPolicyCommands")
    @DisplayName("Negative : MARKET-BID 입력 정책을 위반하면 BusinessException을 반환한다.")
    void create_market_bid_order_with_invalid_policy_inputs(String caseName, Order.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MARKET_BID_INPUT);
    }

    private static Stream<Arguments> invalidMarketBidPolicyCommands() {
        Market market = fixtureMarket();
        User user = fixtureUser();

        return Stream.of(
                Arguments.of("price provided", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.MARKET, null, new BigDecimal("10000"), null, new BigDecimal("10000")
                )),
                Arguments.of("quantity provided", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.MARKET, null, null, BigDecimal.ONE, new BigDecimal("10000")
                )),
                Arguments.of("quoteAmount null", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.MARKET, null, null, null, null
                )),
                Arguments.of("quoteAmount is zero", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.MARKET, null, null, null, BigDecimal.ZERO
                )),
                Arguments.of("quoteAmount is negative", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.MARKET, null, null, null, new BigDecimal("-1")
                ))

        );
    }



    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidMarketAskPolicyCommands")
    @DisplayName("Negative : MARKET-ASK 입력 정책을 위반하면 BusinessException을 반환한다.")
    void create_market_ask_order_with_invalid_policy_inputs(String caseName, Order.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MARKET_ASK_INPUT);
    }

    private static Stream<Arguments> invalidMarketAskPolicyCommands() {
        Market market = fixtureMarket();
        User user = fixtureUser();

        return Stream.of(
                Arguments.of("price provided", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.MARKET, null, new BigDecimal("10000"), BigDecimal.ONE, null
                )),
                Arguments.of("quantity null", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.MARKET, null, null, null, null
                )),
                Arguments.of("quoteAmount provided", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.MARKET, null, null, BigDecimal.ONE, new BigDecimal("10000")
                )),
                Arguments.of("quantity is zero", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.MARKET, null, null, BigDecimal.ZERO, null
                )),
                Arguments.of("quantity is negative", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.MARKET, null, null, new BigDecimal("-1"), null
                ))
        );
    }


    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidTickSizePolicyCommands")
    @DisplayName("Negative : LIMIT 주문이 tick_size 정책을 위반하면 BusinessException을 반환한다.")
    void create_limit_order_with_invalid_tick_size_policy(
            String caseName, Order.CreateCommand command, ErrorCode errorCode
    ) {
        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private static Stream<Arguments> invalidTickSizePolicyCommands() {
        Market market = fixtureMarket();
        User user = fixtureUser();

        return Stream.of(
                Arguments.of("LIMIT-BID tick_size mismatch", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("10500"), BigDecimal.ONE, null
                ), ErrorCode.INVALID_LIMIT_BID_INPUT),
                Arguments.of("LIMIT-ASK tick_size mismatch", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.LIMIT, null, new BigDecimal("10500"), BigDecimal.ONE, null
                ), ErrorCode.INVALID_LIMIT_ASK_INPUT)
        );
    }



    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidMinOrderQuotePolicyCommands")
    @DisplayName("Negative : 주문 금액이 min_order_quote 정책을 위반하면 BusinessException을 반환한다.")
    void create_order_with_invalid_min_order_quote_policy(
            String caseName, Order.CreateCommand command, ErrorCode errorCode
    ) {
        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private static Stream<Arguments> invalidMinOrderQuotePolicyCommands() {
        Market market = fixtureMarket();
        User user = fixtureUser();

        return Stream.of(
                Arguments.of("LIMIT-BID min_order_quote mismatch", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.LIMIT, null, new BigDecimal("4000"), BigDecimal.ONE, null
                ), ErrorCode.INVALID_LIMIT_BID_INPUT),
                Arguments.of("LIMIT-ASK min_order_quote mismatch", createCommand(
                        market, user, "client-order-1", OrderSide.ASK, OrderType.LIMIT, null, new BigDecimal("4000"), BigDecimal.ONE, null
                ), ErrorCode.INVALID_LIMIT_ASK_INPUT),
                Arguments.of("MARKET-BID min_order_quote mismatch", createCommand(
                        market, user, "client-order-1", OrderSide.BID, OrderType.MARKET, null, null, null, new BigDecimal("4000")
                ), ErrorCode.INVALID_MARKET_BID_INPUT)
        );
    }


    @Test
    @DisplayName("Negative : FOK를 넣으면 BusinessException을 반환한다.")
    void create_order_with_unsupported_time_in_force() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.LIMIT,
                TimeInForce.FOK, price, quantity, null
        );

        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNSUPPORTED_TIME_IN_FORCE);
    }

    @Test
    @DisplayName("Negative : MARKET 주문에 GTC를 넣으면 BusinessException을 반환한다.")
    void create_market_order_with_invalid_time_in_force() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.MARKET,
                TimeInForce.GTC, null, null, new BigDecimal("7000")
        );

        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_INPUT);
    }

    @Test
    @DisplayName("Negative : LIMIT 주문에 IOC를 넣으면 BusinessException을 반환한다.")
    void create_limit_order_with_invalid_time_in_force() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.LIMIT,
                TimeInForce.IOC, price, quantity, null
        );

        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_INPUT);
    }

    @Test
    @DisplayName("Negative : LIMIT-BID 수량 scale이 base decimals를 초과하면 BusinessException을 반환한다.")
    void create_limit_bid_order_with_quantity_scale_overflow() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.LIMIT,
                null, price, new BigDecimal("1.123456789"), null
        );

        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LIMIT_BID_INPUT);
    }

    @Test
    @DisplayName("Negative : LIMIT-ASK 수량 scale이 base decimals를 초과하면 BusinessException을 반환한다.")
    void create_limit_ask_order_with_quantity_scale_overflow() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.LIMIT,
                null, price, new BigDecimal("1.123456789"), null
        );

        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LIMIT_ASK_INPUT);
    }

    @Test
    @DisplayName("Negative : MARKET-BID quoteAmount scale이 quote decimals를 초과하면 BusinessException을 반환한다.")
    void create_market_bid_order_with_quote_amount_scale_overflow() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.MARKET,
                null, null, null, new BigDecimal("10000.123")
        );

        // when & then
        assertThatThrownBy(() -> Order.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MARKET_BID_INPUT);
    }

    @Test
    @DisplayName("Happy : trailing zero가 있는 LIMIT-BID 수량은 허용된다.")
    void create_limit_bid_order_with_trailing_zero_quantity() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.LIMIT,
                null, price, new BigDecimal("1.230000000"), null
        );

        // when
        Order order = Order.create(command);

        // then
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
    }

    @Test
    @DisplayName("Happy : trailing zero가 있는 MARKET-BID quoteAmount는 허용된다.")
    void create_market_bid_order_with_trailing_zero_quote_amount() {
        // given
        Order.CreateCommand command = createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.MARKET,
                null, null, null, new BigDecimal("10000.1000")
        );

        // when
        Order order = Order.create(command);

        // then
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
    }

    @Test
    @DisplayName("Happy : 부분 체결 시 OPEN 상태를 유지하고 executed 값이 누적된다.")
    void apply_executed_quantity_partial_fill_keeps_open() {
        // given
        Order order = Order.create(createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.LIMIT,
                null, price, new BigDecimal("2"), null
        ));

        // when
        order.applyExecutedQuantity(BigDecimal.ONE, new BigDecimal("10000"));

        // then
        assertThat(order.getExecutedQuantity()).isEqualByComparingTo("1");
        assertThat(order.getExecutedQuoteAmount()).isEqualByComparingTo("10000");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(order.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("Happy : LIMIT 주문이 전량 체결되면 FILLED 상태로 변경된다.")
    void apply_executed_quantity_full_fill_changes_to_filled() {
        // given
        Order order = Order.create(createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.LIMIT,
                null, price, new BigDecimal("2"), null
        ));

        // when
        order.applyExecutedQuantity(new BigDecimal("2"), new BigDecimal("20000"));

        // then
        assertThat(order.getExecutedQuantity()).isEqualByComparingTo("2");
        assertThat(order.getExecutedQuoteAmount()).isEqualByComparingTo("20000");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("Happy : MARKET-BID는 executedQuoteAmount 기준으로 FILLED 상태가 된다.")
    void apply_executed_quantity_market_bid_uses_executed_quote_amount() {
        // given
        Order order = Order.create(createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.MARKET,
                null, null, null, new BigDecimal("10000")
        ));

        // when
        order.applyExecutedQuantity(new BigDecimal("0.5"), new BigDecimal("10000"));

        // then
        assertThat(order.getExecutedQuantity()).isEqualByComparingTo("0.5");
        assertThat(order.getExecutedQuoteAmount()).isEqualByComparingTo("10000");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    @DisplayName("Negative : OPEN 상태가 아닌 주문에 체결을 반영하면 BusinessException을 반환한다.")
    void apply_executed_quantity_on_not_open_order() {
        // given
        Order order = Order.create(createCommand(
                market, user, clientOrderId, OrderSide.BID, OrderType.LIMIT,
                null, price, BigDecimal.ONE, null
        ));
        order.cancel("user cancel");
        BigDecimal executedQuantity = BigDecimal.ONE;
        BigDecimal executionPrice = new BigDecimal("10000");

        // when & then
        assertThatThrownBy(() -> order.applyExecutedQuantity(executedQuantity, executionPrice))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_OPEN);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidApplyExecutedQuantityInputs")
    @DisplayName("Negative : 체결 반영 입력값이 유효하지 않으면 BusinessException을 반환한다.")
    void apply_executed_quantity_with_invalid_inputs(
            String caseName,
            BigDecimal executedQuantity,
            BigDecimal executedQuoteAmount
    ) {
        // given
        Order order = Order.create(createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.LIMIT,
                null, price, new BigDecimal("2"), null
        ));

        // when & then
        assertThatThrownBy(() -> order.applyExecutedQuantity(executedQuantity, executedQuoteAmount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRADE_INPUT);
    }

    private static Stream<Arguments> invalidApplyExecutedQuantityInputs() {
        return Stream.of(
                Arguments.of("executedQuantity null", null, BigDecimal.ONE),
                Arguments.of("executedQuoteAmount null", BigDecimal.ONE, null),
                Arguments.of("executedQuantity zero", BigDecimal.ZERO, BigDecimal.ONE),
                Arguments.of("executedQuoteAmount zero", BigDecimal.ONE, BigDecimal.ZERO),
                Arguments.of("executedQuantity negative", new BigDecimal("-1"), BigDecimal.ONE),
                Arguments.of("executedQuoteAmount negative", BigDecimal.ONE, new BigDecimal("-1"))
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("cancelOpenOrderInputs")
    @DisplayName("Happy : OPEN 주문 cancel 시 입력 사유에 따라 취소 사유가 저장된다.")
    void cancel_open_order_with_reason(
            String caseName,
            String reason,
            String expectedCancelReason
    ) {
        // given
        Order order = Order.create(createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.LIMIT,
                null, price, BigDecimal.ONE, null
        ));

        // when
        order.cancel(reason);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCancelReason()).isEqualTo(expectedCancelReason);
    }

    private static Stream<Arguments> cancelOpenOrderInputs() {
        return Stream.of(
                Arguments.of("reason null", null, "Order Canceled"),
                Arguments.of("reason blank", "   ", "Order Canceled"),
                Arguments.of("reason custom", "USER_REQUEST", "USER_REQUEST")
        );
    }

    @Test
    @DisplayName("Happy : 이미 FILLED 상태인 주문에 cancel을 호출해도 상태는 유지된다.")
    void cancel_filled_order_is_no_op() {
        // given
        Order order = Order.create(createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.LIMIT,
                null, price, BigDecimal.ONE, null
        ));
        order.applyExecutedQuantity(BigDecimal.ONE, new BigDecimal("10000"));

        // when
        order.cancel("USER_REQUEST");

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("Happy : 이미 CANCELED 상태인 주문에 cancel을 다시 호출해도 기존 사유를 유지한다.")
    void cancel_canceled_order_is_no_op() {
        // given
        Order order = Order.create(createCommand(
                market, user, clientOrderId, OrderSide.ASK, OrderType.LIMIT,
                null, price, BigDecimal.ONE, null
        ));
        order.cancel("FIRST_REASON");

        // when
        order.cancel("SECOND_REASON");

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCancelReason()).isEqualTo("FIRST_REASON");
    }


    // 사용자 생성 헬퍼
    private static User fixtureUser() {
        return User.create("test@test.com", "test", EnumStatus.ACTIVE, "password");
    }

    // 마켓 생성 헬퍼
    private static Market fixtureMarket() {
        Asset baseAsset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Asset quoteAsset = Asset.create("KRW", "Korean Won", (byte) 2, EnumStatus.ACTIVE);
        return Market.create(new Market.CreateCommand(
                baseAsset,
                quoteAsset,
                EnumStatus.ACTIVE,
                new BigDecimal("5000"),
                new BigDecimal("1000")
        ));
    }

    // 커맨드 핼퍼
    private static Order.CreateCommand createCommand(
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
        return new Order.CreateCommand(
                market, user, clientOrderId, orderSide, orderType, timeInForce, price, quantity, quoteAmount
        );
    }
}
