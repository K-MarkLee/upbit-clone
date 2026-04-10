package com.project.upbit_clone.trade.domain.model;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Trade 도메인 테스트")
class TradeTest {
    private Market market;
    private Order buyOrder;
    private Order sellOrder;

    @BeforeEach
    void setUp() {
        TradeFixture fixture = fixture();
        market = fixture.market();
        buyOrder = fixture.buyOrder();
        sellOrder = fixture.sellOrder();
    }

    @Test
    @DisplayName("Happy : 유효한 값을 넣고 생성하면 체결이 생성된다.")
    void create_trade_with_valid_inputs() {
        // given
        Trade.CreateCommand command = createCommand(
                market, buyOrder, sellOrder, OrderSide.ASK,
                sellOrder.getPrice(), BigDecimal.ONE, new BigDecimal("10000"),
                new BigDecimal("0.0010"), new BigDecimal("10"), new BigDecimal("10")
        );

        // when
        Trade trade = Trade.create(command);

        // then
        assertThat(trade).isNotNull();
        assertThat(trade.getMarket()).isEqualTo(market);
        assertThat(trade.getBuyOrder()).isEqualTo(buyOrder);
        assertThat(trade.getSellOrder()).isEqualTo(sellOrder);
        assertThat(trade.getTradeKey()).startsWith("trade-key-");
        assertThat(trade.getMakerOrderSide()).isEqualTo(OrderSide.ASK);
        assertThat(trade.getPrice()).isEqualTo(new BigDecimal("10000"));
        assertThat(trade.getQuantity()).isEqualTo(BigDecimal.ONE);
        assertThat(trade.getQuoteAmount()).isEqualTo(new BigDecimal("10000"));
        assertThat(trade.getFeeRate()).isEqualTo(new BigDecimal("0.0010"));
        assertThat(trade.getBuyFeeAmount()).isEqualTo(new BigDecimal("10"));
        assertThat(trade.getSellFeeAmount()).isEqualTo(new BigDecimal("10"));
    }

    @Test
    @DisplayName("Happy : fee 관련 입력이 null이면 기본값으로 생성된다.")
    void create_trade_with_null_fee_inputs() {
        // given
        Trade.CreateCommand command = createCommand(
                market, buyOrder, sellOrder, OrderSide.ASK,
                sellOrder.getPrice(), BigDecimal.ONE, new BigDecimal("10000"),
                null, null, null
        );

        // when
        Trade trade = Trade.create(command);

        // then
        assertThat(trade).isNotNull();
        assertThat(trade.getFeeRate()).isEqualTo(new BigDecimal("0.0005"));
        assertThat(trade.getBuyFeeAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(trade.getSellFeeAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Happy : makerOrderSide가 BID이면 체결가는 buyOrder 가격으로 생성된다.")
    void create_trade_with_bid_maker_price() {
        // given
        Trade.CreateCommand command = createCommand(
                market, buyOrder, sellOrder, OrderSide.BID,
                buyOrder.getPrice(), BigDecimal.ONE, new BigDecimal("11000"),
                null, null, null
        );

        // when
        Trade trade = Trade.create(command);

        // then
        assertThat(trade).isNotNull();
        assertThat(trade.getMakerOrderSide()).isEqualTo(OrderSide.BID);
        assertThat(trade.getPrice()).isEqualTo(buyOrder.getPrice());
    }

    @Test
    @DisplayName("Negative : trade_key가 128자를 초과하면 BusinessException을 반환한다.")
    void create_trade_with_too_long_trade_key() {
        Trade.CreateCommand command = new Trade.CreateCommand(
                market,
                buyOrder,
                sellOrder,
                "t".repeat(129),
                OrderSide.ASK,
                sellOrder.getPrice(),
                BigDecimal.ONE,
                new BigDecimal("10000"),
                null,
                null,
                null
        );

        assertThatThrownBy(() -> Trade.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRADE_INPUT);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullRequiredTradeCommands")
    @DisplayName("Negative : 필수 입력값이 null이면 BusinessException을 반환한다.")
    void create_trade_with_null_required_inputs(String caseName, Trade.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> Trade.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRADE_INPUT);
    }

    private static Stream<Arguments> nullRequiredTradeCommands() {
        TradeFixture fixture = fixture();
        return Stream.of(
                Arguments.of("command null", null),
                Arguments.of("tradeKey null", new Trade.CreateCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), null, OrderSide.ASK,
                        fixture.sellOrder().getPrice(), BigDecimal.ONE, new BigDecimal("10000"), null, null, null
                )),
                Arguments.of("market null", createCommand(
                        null, fixture.buyOrder(), fixture.sellOrder(), OrderSide.ASK, fixture.sellOrder().getPrice(),
                        BigDecimal.ONE, new BigDecimal("10000"), null, null, null
                )),
                Arguments.of("buyOrder null", new Trade.CreateCommand(
                        fixture.market(), null, fixture.sellOrder(), "trade-key-buy-null", OrderSide.ASK,
                        fixture.sellOrder().getPrice(), BigDecimal.ONE, new BigDecimal("10000"), null, null, null
                )),
                Arguments.of("sellOrder null", new Trade.CreateCommand(
                        fixture.market(), fixture.buyOrder(), null, "trade-key-sell-null", OrderSide.ASK,
                        fixture.sellOrder().getPrice(), BigDecimal.ONE, new BigDecimal("10000"), null, null, null
                )),
                Arguments.of("makerOrderSide null", new Trade.CreateCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), "trade-key-maker-null", null,
                        fixture.sellOrder().getPrice(), BigDecimal.ONE, new BigDecimal("10000"), null, null, null
                )),
                Arguments.of("price null", new Trade.CreateCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), "trade-key-price-null", OrderSide.ASK,
                        null, BigDecimal.ONE, new BigDecimal("10000"), null, null, null
                )),
                Arguments.of("quantity null", new Trade.CreateCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), "trade-key-quantity-null", OrderSide.ASK,
                        fixture.sellOrder().getPrice(), null, new BigDecimal("10000"), null, null, null
                )),
                Arguments.of("quoteAmount null", new Trade.CreateCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), "trade-key-quote-null", OrderSide.ASK,
                        fixture.sellOrder().getPrice(), BigDecimal.ONE, null, null, null, null
                ))
        );
    }


    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidOrderSideCommands")
    @DisplayName("Negative : buyOrder/sellOrder의 orderSide가 유효하지 않으면 BusinessException을 반환한다.")
    void create_trade_with_invalid_order_side(String caseName, Trade.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> Trade.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_SIDE_NOT_MATCHED);
    }

    private static Stream<Arguments> invalidOrderSideCommands() {
        TradeFixture fixture = fixture();
        Order invalidBuyOrder = createLimitAskOrder(
                fixture.market(), fixture.buyUser(), "invalid-buy", new BigDecimal("12000"), BigDecimal.ONE
        );
        Order invalidSellOrder = createLimitBidOrder(
                fixture.market(), fixture.sellUser(), "invalid-sell", new BigDecimal("9000"), BigDecimal.ONE
        );

        return Stream.of(
                Arguments.of("buyOrder side is ASK", createCommand(
                        fixture.market(), invalidBuyOrder, fixture.sellOrder(), OrderSide.ASK, fixture.sellOrder().getPrice(),
                        BigDecimal.ONE, new BigDecimal("10000"), null, null, null
                )),
                Arguments.of("sellOrder side is BID", createCommand(
                        fixture.market(), fixture.buyOrder(), invalidSellOrder, OrderSide.BID, fixture.buyOrder().getPrice(),
                        BigDecimal.ONE, new BigDecimal("11000"), null, null, null
                ))
        );
    }


    @Test
    @DisplayName("Negative : self-trade 조건이면 BusinessException을 반환한다.")
    void create_trade_with_self_trade() {
        // given
        User sameUser = User.create("same@test.com", "same-user", EnumStatus.ACTIVE, "same-password");
        setUserId(sameUser, 10L);

        Order sameUserBuyOrder = createLimitBidOrder(
                market, sameUser, "same-buy", new BigDecimal("11000"), BigDecimal.ONE
        );
        Order sameUserSellOrder = createLimitAskOrder(
                market, sameUser, "same-sell", new BigDecimal("10000"), BigDecimal.ONE
        );
        Trade.CreateCommand command = createCommand(
                market, sameUserBuyOrder, sameUserSellOrder, OrderSide.ASK,
                sameUserSellOrder.getPrice(), BigDecimal.ONE, new BigDecimal("10000"),
                null, null, null
        );

        // when & then
        assertThatThrownBy(() -> Trade.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SELF_TRADE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("Negative : trade.market과 order.market이 다르면 BusinessException을 반환한다.")
    void create_trade_with_market_mismatch() {
        // given
        Asset baseAsset = Asset.create("ETH", "Ethereum", (byte) 8, EnumStatus.ACTIVE);
        Asset quoteAsset = Asset.create("KRW", "Korean Won", (byte) 2, EnumStatus.ACTIVE);
        Market differentMarket = Market.create(new Market.CreateCommand(
                baseAsset,
                quoteAsset,
                EnumStatus.ACTIVE,
                new BigDecimal("5000"),
                new BigDecimal("1000")
        ));
        Trade.CreateCommand command = createCommand(
                differentMarket, buyOrder, sellOrder, OrderSide.ASK,
                sellOrder.getPrice(), BigDecimal.ONE, new BigDecimal("10000"),
                null, null, null
        );

        // when & then
        assertThatThrownBy(() -> Trade.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRADE_MARKET_NOT_MATCHED);
    }

    @Test
    @DisplayName("Happy : trade.market과 order.market이 서로 다른 인스턴스여도 id가 같으면 생성된다.")
    void create_trade_with_same_market_id_different_instance() {
        // given
        setMarketId(market, 1L);

        Asset baseAsset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Asset quoteAsset = Asset.create("KRW", "Korean Won", (byte) 2, EnumStatus.ACTIVE);
        Market sameIdMarket = Market.create(new Market.CreateCommand(
                baseAsset,
                quoteAsset,
                EnumStatus.ACTIVE,
                new BigDecimal("5000"),
                new BigDecimal("1000")
        ));
        setMarketId(sameIdMarket, 1L);

        Trade.CreateCommand command = createCommand(
                sameIdMarket, buyOrder, sellOrder, OrderSide.ASK,
                sellOrder.getPrice(), BigDecimal.ONE, new BigDecimal("10000"),
                null, null, null
        );

        // when
        Trade trade = Trade.create(command);

        // then
        assertThat(trade).isNotNull();
        assertThat(trade.getMarket()).isEqualTo(sameIdMarket);
    }

    @Test
    @DisplayName("Negative : trade.market과 order.market이 서로 다른 인스턴스이고 id도 다르면 BusinessException을 반환한다.")
    void create_trade_with_different_market_id_different_instance() {
        // given
        setMarketId(market, 1L);

        Asset baseAsset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Asset quoteAsset = Asset.create("KRW", "Korean Won", (byte) 2, EnumStatus.ACTIVE);
        Market differentIdMarket = Market.create(new Market.CreateCommand(
                baseAsset,
                quoteAsset,
                EnumStatus.ACTIVE,
                new BigDecimal("5000"),
                new BigDecimal("1000")
        ));
        setMarketId(differentIdMarket, 2L);

        Trade.CreateCommand command = createCommand(
                differentIdMarket, buyOrder, sellOrder, OrderSide.ASK,
                sellOrder.getPrice(), BigDecimal.ONE, new BigDecimal("10000"),
                null, null, null
        );

        // when & then
        assertThatThrownBy(() -> Trade.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRADE_MARKET_NOT_MATCHED);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidMakerPriceCommands")
    @DisplayName("Negative : 체결가가 maker 주문 가격과 다르면 BusinessException을 반환한다.")
    void create_trade_with_invalid_maker_price(String caseName, Trade.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> Trade.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRADE_PRICE_MUST_BE_MAKER_PRICE);
    }

    private static Stream<Arguments> invalidMakerPriceCommands() {
        TradeFixture fixture = fixture();
        return Stream.of(
                Arguments.of("maker ASK but buyOrder price is used", createCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), OrderSide.ASK,
                        fixture.buyOrder().getPrice(), BigDecimal.ONE, new BigDecimal("11000"),
                        null, null, null
                )),
                Arguments.of("maker BID but sellOrder price is used", createCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), OrderSide.BID,
                        fixture.sellOrder().getPrice(), BigDecimal.ONE, new BigDecimal("10000"),
                        null, null, null
                ))
        );
    }


    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonPositiveAmountCommands")
    @DisplayName("Negative : quantity 또는 quoteAmount가 0 이하면 IllegalArgumentException을 반환한다.")
    void create_trade_with_non_positive_amounts(String caseName, Trade.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> Trade.create(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> nonPositiveAmountCommands() {
        TradeFixture fixture = fixture();
        return Stream.of(
                Arguments.of("quantity is zero", createCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), OrderSide.ASK, fixture.sellOrder().getPrice(),
                        BigDecimal.ZERO, new BigDecimal("10000"), null, null, null
                )),
                Arguments.of("quoteAmount is zero", createCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), OrderSide.ASK, fixture.sellOrder().getPrice(),
                        BigDecimal.ONE, BigDecimal.ZERO, null, null, null
                ))
        );
    }


    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("negativeFeeCommands")
    @DisplayName("Negative : feeRate 또는 feeAmount가 음수면 IllegalArgumentException을 반환한다.")
    void create_trade_with_negative_fee_inputs(String caseName, Trade.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> Trade.create(command))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> negativeFeeCommands() {
        TradeFixture fixture = fixture();
        return Stream.of(
                Arguments.of("feeRate is negative", createCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), OrderSide.ASK, fixture.sellOrder().getPrice(),
                        BigDecimal.ONE, new BigDecimal("10000"), new BigDecimal("-0.0001"), null, null
                )),
                Arguments.of("buyFeeAmount is negative", createCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), OrderSide.ASK, fixture.sellOrder().getPrice(),
                        BigDecimal.ONE, new BigDecimal("10000"), null, new BigDecimal("-1"), null
                )),
                Arguments.of("sellFeeAmount is negative", createCommand(
                        fixture.market(), fixture.buyOrder(), fixture.sellOrder(), OrderSide.ASK, fixture.sellOrder().getPrice(),
                        BigDecimal.ONE, new BigDecimal("10000"), null, null, new BigDecimal("-1")
                ))
        );
    }


    // asset, user, order 생성 헬퍼
    private static TradeFixture fixture() {
        Asset baseAsset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Asset quoteAsset = Asset.create("KRW", "Korean Won", (byte) 2, EnumStatus.ACTIVE);
        Market market = Market.create(new Market.CreateCommand(
                baseAsset,
                quoteAsset,
                EnumStatus.ACTIVE,
                new BigDecimal("5000"),
                new BigDecimal("1000")
        ));

        User buyUser = User.create("buyer@test.com", "buyer", EnumStatus.ACTIVE, "buyer-password");
        User sellUser = User.create("seller@test.com", "seller", EnumStatus.ACTIVE, "seller-password");
        setUserId(buyUser, 1L);
        setUserId(sellUser, 2L);

        Order buyOrder = createLimitBidOrder(market, buyUser, "buy-1", new BigDecimal("11000"), BigDecimal.ONE);
        Order sellOrder = createLimitAskOrder(market, sellUser, "sell-1", new BigDecimal("10000"), BigDecimal.ONE);
        return new TradeFixture(market, buyUser, sellUser, buyOrder, sellOrder);
    }

    // LIMIT - BID 헬퍼
    private static Order createLimitBidOrder(
            Market market,
            User user,
            String clientOrderId,
            BigDecimal price,
            BigDecimal quantity
    ) {
        return Order.create(new Order.CreateCommand(
                market, user, clientOrderId, nextOrderKey(clientOrderId), OrderSide.BID, OrderType.LIMIT,
                null, price, quantity, null
        ));
    }

    // LIMIT - ASK 헬퍼
    private static Order createLimitAskOrder(
            Market market,
            User user,
            String clientOrderId,
            BigDecimal price,
            BigDecimal quantity
    ) {
        return Order.create(new Order.CreateCommand(
                market, user, clientOrderId, nextOrderKey(clientOrderId), OrderSide.ASK, OrderType.LIMIT,
                null, price, quantity, null
        ));
    }

    // 커맨드 헬퍼
    private static Trade.CreateCommand createCommand(
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
        return new Trade.CreateCommand(
                market,
                buyOrder,
                sellOrder,
                nextTradeKey(buyOrder.getClientOrderId(), sellOrder.getClientOrderId()),
                makerOrderSide,
                price,
                quantity,
                quoteAmount,
                feeRate,
                buyFeeAmount,
                sellFeeAmount
        );
    }

    private static String nextOrderKey(String clientOrderId) {
        return "order-key-" + clientOrderId;
    }

    private static String nextTradeKey(String buyClientOrderId, String sellClientOrderId) {
        return "trade-key-" + buyClientOrderId + "-" + sellClientOrderId;
    }

    // 유저 헬퍼 (다른 이용자 id)
    private static void setUserId(User user, Long userId) {
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, userId);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("User id 필드 주입 실패", e);
        }
    }

    // 마켓 id 헬퍼 (isDifferentMarket의 id 비교 분기 테스트용)
    private static void setMarketId(Market market, Long marketId) {
        try {
            Field idField = Market.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(market, marketId);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Market id 필드 주입 실패", e);
        }
    }

    // 거래 헬퍼
    private record TradeFixture(
            Market market,
            User buyUser,
            User sellUser,
            Order buyOrder,
            Order sellOrder
    ) {
    }
}
