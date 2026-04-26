package com.project.upbit_clone.api;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.repository.TradeRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.presentation.request.OrderRequest;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import com.project.upbit_clone.wallet.domain.model.Wallet;
import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("API 엔드투엔드 테스트")
class ApiEndToEndTest {

    private static final BigDecimal ORDER_PRICE = new BigDecimal("100");
    private static final BigDecimal ORDER_QUANTITY = new BigDecimal("0.2");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MarketRepository marketRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private User seller;
    private User buyer;
    private Asset btcAsset;
    private Asset krwAsset;
    private Market market;
    private Wallet sellerBtcWallet;
    private Wallet sellerKrwWallet;
    private Wallet buyerBtcWallet;
    private Wallet buyerKrwWallet;

    @BeforeEach
    void setUp() {
        SeedData seedData = transactionTemplate.execute(status -> {
            User savedSeller = userRepository.saveAndFlush(User.create(
                    "seller@test.com",
                    "seller",
                    EnumStatus.ACTIVE,
                    "seller-password"
            ));
            User savedBuyer = userRepository.saveAndFlush(User.create(
                    "buyer@test.com",
                    "buyer",
                    EnumStatus.ACTIVE,
                    "buyer-password"
            ));

            Asset savedBtcAsset = persistAsset("BTC", "Bitcoin", (byte) 8);
            Asset savedKrwAsset = persistAsset("KRW", "Korean Won", (byte) 0);

            Market savedMarket = marketRepository.saveAndFlush(Market.create(new Market.CreateCommand(
                    savedBtcAsset,
                    savedKrwAsset,
                    EnumStatus.ACTIVE,
                    new BigDecimal("10"),
                    new BigDecimal("10")
            )));

            Wallet savedSellerBtcWallet = walletRepository.saveAndFlush(Wallet.create(
                    savedSeller,
                    savedBtcAsset,
                    new BigDecimal("1.0"),
                    BigDecimal.ZERO
            ));
            Wallet savedSellerKrwWallet = walletRepository.saveAndFlush(Wallet.create(
                    savedSeller,
                    savedKrwAsset,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            ));
            Wallet savedBuyerBtcWallet = walletRepository.saveAndFlush(Wallet.create(
                    savedBuyer,
                    savedBtcAsset,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            ));
            Wallet savedBuyerKrwWallet = walletRepository.saveAndFlush(Wallet.create(
                    savedBuyer,
                    savedKrwAsset,
                    new BigDecimal("1000"),
                    BigDecimal.ZERO
            ));

            return new SeedData(
                    savedSeller,
                    savedBuyer,
                    savedBtcAsset,
                    savedKrwAsset,
                    savedMarket,
                    savedSellerBtcWallet,
                    savedSellerKrwWallet,
                    savedBuyerBtcWallet,
                    savedBuyerKrwWallet
            );
        });

        if (seedData == null) {
            fail("시드 데이터 생성에 실패했습니다.");
        }

        seller = seedData.seller();
        buyer = seedData.buyer();
        btcAsset = seedData.btcAsset();
        krwAsset = seedData.krwAsset();
        market = seedData.market();
        sellerBtcWallet = seedData.sellerBtcWallet();
        sellerKrwWallet = seedData.sellerKrwWallet();
        buyerBtcWallet = seedData.buyerBtcWallet();
        buyerKrwWallet = seedData.buyerKrwWallet();

        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from order_book_projection");
        jdbcTemplate.update("delete from consumer_offset");
        jdbcTemplate.update("delete from event_log");
        jdbcTemplate.update("delete from ledger");
        jdbcTemplate.update("delete from trade");
        jdbcTemplate.update("delete from command_log");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from wallet");
        jdbcTemplate.update("delete from market");
        jdbcTemplate.update("delete from asset");
        jdbcTemplate.update("delete from users");

        entityManager.clear();
    }

    @Test
    @DisplayName("Happy : 시장/지갑 조회 API는 beforeEach로 넣은 시드 데이터를 반환한다.")
    void find_market_and_wallets_with_seed_data() throws Exception {
        JsonNode markets = successData(mockMvc.perform(
                get("/api/v1/markets")
        ).andExpect(status().isOk()).andReturn());

        assertThat(markets.size()).isEqualTo(1);
        assertThat(markets.get(0).path("marketId").asLong()).isEqualTo(market.getId());
        assertThat(markets.get(0).path("marketCode").asString()).isEqualTo("KRW-BTC");
        assertThat(markets.get(0).path("baseAsset").path("symbol").asString()).isEqualTo("BTC");
        assertThat(markets.get(0).path("quoteAsset").path("symbol").asString()).isEqualTo("KRW");

        JsonNode marketDetail = successData(mockMvc.perform(
                get("/api/v1/markets/{marketId}", market.getId())
        ).andExpect(status().isOk()).andReturn());

        assertThat(marketDetail.path("marketId").asLong()).isEqualTo(market.getId());
        assertThat(marketDetail.path("minOrderQuote").decimalValue()).isEqualByComparingTo("10");
        assertThat(marketDetail.path("tickSize").decimalValue()).isEqualByComparingTo("10");

        JsonNode wallets = successData(mockMvc.perform(
                get("/api/v1/wallets")
                        .param("userId", seller.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(wallets.size()).isEqualTo(2);
        assertThat(wallets.get(0).path("assetSymbol").asString()).isEqualTo("BTC");
        assertThat(wallets.get(0).path("availableBalance").decimalValue()).isEqualByComparingTo("1.0");
        assertThat(wallets.get(0).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");
        assertThat(wallets.get(1).path("assetSymbol").asString()).isEqualTo("KRW");
        assertThat(wallets.get(1).path("availableBalance").decimalValue()).isEqualByComparingTo("0");
        assertThat(wallets.get(1).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Happy : 지정가 매도 주문을 넣으면 OPEN 주문, 잠금 잔고, 원장 조회가 함께 반영된다.")
    void place_limit_ask_order_and_query_order_wallet_and_ledger() throws Exception {
        JsonNode placeResponse = successData(placeLimitAsk("ask-open-1"));

        assertThat(placeResponse.path("commandType").asString()).isEqualTo("PLACE_ORDER");
        assertThat(placeResponse.path("status").asString()).isEqualTo("ACCEPTED");
        assertThat(placeResponse.path("idempotencyHit").asBoolean()).isFalse();

        awaitOrderStatus(seller.getId(), "ask-open-1", OrderStatus.OPEN);

        JsonNode orderList = successData(mockMvc.perform(
                get("/api/v1/orders")
                        .param("userId", seller.getId().toString())
                        .param("marketId", market.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(orderList.size()).isEqualTo(1);
        assertThat(orderList.get(0).path("clientOrderId").asString()).isEqualTo("ask-open-1");
        assertThat(orderList.get(0).path("status").asString()).isEqualTo("OPEN");
        assertThat(orderList.get(0).path("orderSide").asString()).isEqualTo("ASK");
        assertThat(orderList.get(0).path("orderType").asString()).isEqualTo("LIMIT");
        assertThat(orderList.get(0).path("timeInForce").asString()).isEqualTo("GTC");
        assertThat(orderList.get(0).path("price").decimalValue()).isEqualByComparingTo("100");
        assertThat(orderList.get(0).path("quantity").decimalValue()).isEqualByComparingTo("0.2");

        JsonNode orderDetail = successData(mockMvc.perform(
                get("/api/v1/orders/detail")
                        .param("userId", seller.getId().toString())
                        .param("clientOrderId", "ask-open-1")
        ).andExpect(status().isOk()).andReturn());

        assertThat(orderDetail.path("executedQuantity").decimalValue()).isEqualByComparingTo("0");
        assertThat(orderDetail.path("executedQuoteAmount").decimalValue()).isEqualByComparingTo("0");

        JsonNode wallets = successData(mockMvc.perform(
                get("/api/v1/wallets")
                        .param("userId", seller.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(wallets.size()).isEqualTo(2);
        assertThat(wallets.get(0).path("walletId").asLong()).isEqualTo(sellerBtcWallet.getId());
        assertThat(wallets.get(0).path("availableBalance").decimalValue()).isEqualByComparingTo("0.8");
        assertThat(wallets.get(0).path("lockedBalance").decimalValue()).isEqualByComparingTo("0.2");

        JsonNode ledgers = successData(mockMvc.perform(
                get("/api/v1/ledgers")
                        .param("userId", seller.getId().toString())
                        .param("walletId", sellerBtcWallet.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(ledgers.size()).isEqualTo(1);
        assertThat(ledgers.get(0).path("ledgerType").asString()).isEqualTo("ORDER_LOCK");
        assertThat(ledgers.get(0).path("changeType").asString()).isEqualTo("DECREASE");
        assertThat(ledgers.get(0).path("amount").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(ledgers.get(0).path("availableBefore").decimalValue()).isEqualByComparingTo("1.0");
        assertThat(ledgers.get(0).path("availableAfter").decimalValue()).isEqualByComparingTo("0.8");
        assertThat(ledgers.get(0).path("lockedBefore").decimalValue()).isEqualByComparingTo("0");
        assertThat(ledgers.get(0).path("lockedAfter").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(ledgers.get(0).path("description").asString()).isEqualTo("ORDER_LOCK");
    }

    @Test
    @DisplayName("Happy : 매도 OPEN 주문과 매수 주문이 매칭되면 체결/주문/지갑 조회가 함께 반영된다.")
    void match_orders_and_query_trade_order_and_wallets() throws Exception {
        successData(placeLimitAsk("ask-fill-1"));
        awaitOrderStatus(seller.getId(), "ask-fill-1", OrderStatus.OPEN);

        JsonNode placeResponse = successData(placeLimitBid("bid-fill-1"));

        assertThat(placeResponse.path("commandType").asString()).isEqualTo("PLACE_ORDER");
        assertThat(placeResponse.path("status").asString()).isEqualTo("ACCEPTED");

        awaitOrderStatus(seller.getId(), "ask-fill-1", OrderStatus.FILLED);
        awaitOrderStatus(buyer.getId(), "bid-fill-1", OrderStatus.FILLED);
        awaitTradeCount(market.getId(), 1);

        JsonNode sellerOrder = successData(mockMvc.perform(
                get("/api/v1/orders/detail")
                        .param("userId", seller.getId().toString())
                        .param("clientOrderId", "ask-fill-1")
        ).andExpect(status().isOk()).andReturn());

        assertThat(sellerOrder.path("status").asString()).isEqualTo("FILLED");
        assertThat(sellerOrder.path("executedQuantity").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(sellerOrder.path("executedQuoteAmount").decimalValue()).isEqualByComparingTo("20");

        JsonNode buyerOrder = successData(mockMvc.perform(
                get("/api/v1/orders/detail")
                        .param("userId", buyer.getId().toString())
                        .param("clientOrderId", "bid-fill-1")
        ).andExpect(status().isOk()).andReturn());

        assertThat(buyerOrder.path("status").asString()).isEqualTo("FILLED");
        assertThat(buyerOrder.path("executedQuantity").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(buyerOrder.path("executedQuoteAmount").decimalValue()).isEqualByComparingTo("20");

        JsonNode trades = successData(mockMvc.perform(
                get("/api/v1/trades")
                        .param("marketId", market.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(trades.size()).isEqualTo(1);
        assertThat(trades.get(0).path("marketId").asLong()).isEqualTo(market.getId());
        assertThat(trades.get(0).path("marketCode").asString()).isEqualTo("KRW-BTC");
        assertThat(trades.get(0).path("makerOrderSide").asString()).isEqualTo("ASK");
        assertThat(trades.get(0).path("price").decimalValue()).isEqualByComparingTo("100");
        assertThat(trades.get(0).path("quantity").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(trades.get(0).path("quoteAmount").decimalValue()).isEqualByComparingTo("20");

        JsonNode sellerWallets = successData(mockMvc.perform(
                get("/api/v1/wallets")
                        .param("userId", seller.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(sellerWallets.get(0).path("availableBalance").decimalValue()).isEqualByComparingTo("0.8");
        assertThat(sellerWallets.get(0).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");
        assertThat(sellerWallets.get(1).path("availableBalance").decimalValue()).isEqualByComparingTo("20");
        assertThat(sellerWallets.get(1).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");

        JsonNode buyerWallets = successData(mockMvc.perform(
                get("/api/v1/wallets")
                        .param("userId", buyer.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(buyerWallets.get(0).path("availableBalance").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(buyerWallets.get(0).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");
        assertThat(buyerWallets.get(1).path("availableBalance").decimalValue()).isEqualByComparingTo("980");
        assertThat(buyerWallets.get(1).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Happy : OPEN 주문을 취소하면 주문 상태, 잠금 해제, 원장 조회가 함께 반영된다.")
    void cancel_open_order_and_query_order_wallet_and_ledger() throws Exception {
        successData(placeLimitAsk("ask-cancel-1"));
        awaitOrderStatus(seller.getId(), "ask-cancel-1", OrderStatus.OPEN);

        JsonNode cancelResponse = successData(mockMvc.perform(
                post("/api/v1/orders/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new OrderRequest.Cancel(seller.getId(), "ask-cancel-1")))
        ).andExpect(status().isAccepted()).andReturn());

        assertThat(cancelResponse.path("commandType").asString()).isEqualTo("CANCEL_ORDER");
        assertThat(cancelResponse.path("status").asString()).isEqualTo("ACCEPTED");

        awaitOrderStatus(seller.getId(), "ask-cancel-1", OrderStatus.CANCELED);

        JsonNode orderDetail = successData(mockMvc.perform(
                get("/api/v1/orders/detail")
                        .param("userId", seller.getId().toString())
                        .param("clientOrderId", "ask-cancel-1")
        ).andExpect(status().isOk()).andReturn());

        assertThat(orderDetail.path("status").asString()).isEqualTo("CANCELED");
        assertThat(orderDetail.path("cancelReason").asString()).isEqualTo("USER_REQUEST");

        JsonNode wallets = successData(mockMvc.perform(
                get("/api/v1/wallets")
                        .param("userId", seller.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(wallets.get(0).path("availableBalance").decimalValue()).isEqualByComparingTo("1.0");
        assertThat(wallets.get(0).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");

        JsonNode ledgers = successData(mockMvc.perform(
                get("/api/v1/ledgers")
                        .param("userId", seller.getId().toString())
                        .param("walletId", sellerBtcWallet.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(ledgers.size()).isEqualTo(2);
        assertThat(ledgers.get(0).path("ledgerType").asString()).isEqualTo("ORDER_UNLOCK");
        assertThat(ledgers.get(0).path("changeType").asString()).isEqualTo("INCREASE");
        assertThat(ledgers.get(0).path("amount").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(ledgers.get(0).path("description").asString()).isEqualTo("USER_REQUEST");
        assertThat(ledgers.get(1).path("ledgerType").asString()).isEqualTo("ORDER_LOCK");
        assertThat(ledgers.get(1).path("changeType").asString()).isEqualTo("DECREASE");
        assertThat(ledgers.get(1).path("amount").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(ledgers.get(1).path("description").asString()).isEqualTo("ORDER_LOCK");
    }

    @Test
    @DisplayName("Happy : 호가 조회 API는 OPEN 주문의 레벨 생성과 취소 후 제거를 반영한다.")
    void find_order_book_with_open_and_canceled_limit_order() throws Exception {
        successData(placeLimitAsk("ask-book-1"));
        awaitOrderStatus(seller.getId(), "ask-book-1", OrderStatus.OPEN);

        JsonNode openedOrderBook = awaitOrderBookLevelCounts(0, 1);

        assertThat(openedOrderBook.path("marketId").asLong()).isEqualTo(market.getId());
        assertThat(openedOrderBook.path("bids").size()).isZero();
        assertThat(openedOrderBook.path("asks").size()).isEqualTo(1);
        assertThat(openedOrderBook.path("asks").get(0).path("side").asString()).isEqualTo("ASK");
        assertThat(openedOrderBook.path("asks").get(0).path("price").decimalValue()).isEqualByComparingTo("100");
        assertThat(openedOrderBook.path("asks").get(0).path("totalQty").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(openedOrderBook.path("asks").get(0).path("orderCount").asInt()).isEqualTo(1);

        successData(mockMvc.perform(
                post("/api/v1/orders/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new OrderRequest.Cancel(seller.getId(), "ask-book-1")))
        ).andExpect(status().isAccepted()).andReturn());
        awaitOrderStatus(seller.getId(), "ask-book-1", OrderStatus.CANCELED);

        JsonNode canceledOrderBook = awaitOrderBookLevelCounts(0, 0);

        assertThat(canceledOrderBook.path("bids").size()).isZero();
        assertThat(canceledOrderBook.path("asks").size()).isZero();
    }

    @Test
    @DisplayName("Happy : MARKET-BID 주문은 IOC로 정규화되고 체결/지갑 조회가 함께 반영된다.")
    void place_market_bid_and_query_filled_order_trade_and_wallets() throws Exception {
        successData(placeLimitAsk("ask-market-bid-maker-1"));
        awaitOrderStatus(seller.getId(), "ask-market-bid-maker-1", OrderStatus.OPEN);

        JsonNode placeResponse = successData(placeMarketBid("bid-market-1", new BigDecimal("20")));

        assertThat(placeResponse.path("commandType").asString()).isEqualTo("PLACE_ORDER");
        assertThat(placeResponse.path("status").asString()).isEqualTo("ACCEPTED");

        awaitOrderStatus(seller.getId(), "ask-market-bid-maker-1", OrderStatus.FILLED);
        awaitOrderStatus(buyer.getId(), "bid-market-1", OrderStatus.FILLED);
        awaitTradeCount(market.getId(), 1);

        JsonNode buyerOrder = successData(mockMvc.perform(
                get("/api/v1/orders/detail")
                        .param("userId", buyer.getId().toString())
                        .param("clientOrderId", "bid-market-1")
        ).andExpect(status().isOk()).andReturn());

        assertThat(buyerOrder.path("orderSide").asString()).isEqualTo("BID");
        assertThat(buyerOrder.path("orderType").asString()).isEqualTo("MARKET");
        assertThat(buyerOrder.path("timeInForce").asString()).isEqualTo("IOC");
        assertThat(buyerOrder.path("status").asString()).isEqualTo("FILLED");
        assertThat(buyerOrder.path("quoteAmount").decimalValue()).isEqualByComparingTo("20");
        assertThat(buyerOrder.path("executedQuantity").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(buyerOrder.path("executedQuoteAmount").decimalValue()).isEqualByComparingTo("20");

        JsonNode trades = successData(mockMvc.perform(
                get("/api/v1/trades")
                        .param("marketId", market.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(trades.size()).isEqualTo(1);
        assertThat(trades.get(0).path("makerOrderSide").asString()).isEqualTo("ASK");
        assertThat(trades.get(0).path("price").decimalValue()).isEqualByComparingTo("100");
        assertThat(trades.get(0).path("quantity").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(trades.get(0).path("quoteAmount").decimalValue()).isEqualByComparingTo("20");

        JsonNode buyerWallets = successData(mockMvc.perform(
                get("/api/v1/wallets")
                        .param("userId", buyer.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(buyerWallets.get(0).path("availableBalance").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(buyerWallets.get(0).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");
        assertThat(buyerWallets.get(1).path("availableBalance").decimalValue()).isEqualByComparingTo("980");
        assertThat(buyerWallets.get(1).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Happy : MARKET-ASK 주문은 IOC로 정규화되고 체결/지갑 조회가 함께 반영된다.")
    void place_market_ask_and_query_filled_order_trade_and_wallets() throws Exception {
        successData(placeLimitBid("bid-market-ask-maker-1"));
        awaitOrderStatus(buyer.getId(), "bid-market-ask-maker-1", OrderStatus.OPEN);

        JsonNode placeResponse = successData(placeMarketAsk("ask-market-1", ORDER_QUANTITY));

        assertThat(placeResponse.path("commandType").asString()).isEqualTo("PLACE_ORDER");
        assertThat(placeResponse.path("status").asString()).isEqualTo("ACCEPTED");

        awaitOrderStatus(buyer.getId(), "bid-market-ask-maker-1", OrderStatus.FILLED);
        awaitOrderStatus(seller.getId(), "ask-market-1", OrderStatus.FILLED);
        awaitTradeCount(market.getId(), 1);

        JsonNode sellerOrder = successData(mockMvc.perform(
                get("/api/v1/orders/detail")
                        .param("userId", seller.getId().toString())
                        .param("clientOrderId", "ask-market-1")
        ).andExpect(status().isOk()).andReturn());

        assertThat(sellerOrder.path("orderSide").asString()).isEqualTo("ASK");
        assertThat(sellerOrder.path("orderType").asString()).isEqualTo("MARKET");
        assertThat(sellerOrder.path("timeInForce").asString()).isEqualTo("IOC");
        assertThat(sellerOrder.path("status").asString()).isEqualTo("FILLED");
        assertThat(sellerOrder.path("quantity").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(sellerOrder.path("executedQuantity").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(sellerOrder.path("executedQuoteAmount").decimalValue()).isEqualByComparingTo("20");

        JsonNode trades = successData(mockMvc.perform(
                get("/api/v1/trades")
                        .param("marketId", market.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(trades.size()).isEqualTo(1);
        assertThat(trades.get(0).path("makerOrderSide").asString()).isEqualTo("BID");
        assertThat(trades.get(0).path("price").decimalValue()).isEqualByComparingTo("100");
        assertThat(trades.get(0).path("quantity").decimalValue()).isEqualByComparingTo("0.2");
        assertThat(trades.get(0).path("quoteAmount").decimalValue()).isEqualByComparingTo("20");

        JsonNode sellerWallets = successData(mockMvc.perform(
                get("/api/v1/wallets")
                        .param("userId", seller.getId().toString())
        ).andExpect(status().isOk()).andReturn());

        assertThat(sellerWallets.get(0).path("availableBalance").decimalValue()).isEqualByComparingTo("0.8");
        assertThat(sellerWallets.get(0).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");
        assertThat(sellerWallets.get(1).path("availableBalance").decimalValue()).isEqualByComparingTo("20");
        assertThat(sellerWallets.get(1).path("lockedBalance").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Happy/Negative : 동일 주문 재시도는 idempotency hit를 반환하고 다른 payload면 conflict를 반환한다.")
    void place_same_order_again_returns_idempotency_hit_and_conflict_for_different_payload() throws Exception {
        JsonNode firstResponse = successData(placeLimitAsk("ask-idempotency-1"));

        awaitOrderStatus(seller.getId(), "ask-idempotency-1", OrderStatus.OPEN);

        JsonNode secondResponse = successData(placeLimitAsk("ask-idempotency-1"));

        assertThat(secondResponse.path("commandType").asString()).isEqualTo("PLACE_ORDER");
        assertThat(secondResponse.path("status").asString()).isEqualTo("ACCEPTED");
        assertThat(secondResponse.path("idempotencyHit").asBoolean()).isTrue();
        assertThat(secondResponse.path("commandId").asString()).isEqualTo(firstResponse.path("commandId").asString());
        assertThat(secondResponse.path("commandLogId").asLong()).isEqualTo(firstResponse.path("commandLogId").asLong());

        JsonNode failure = failureRoot(mockMvc.perform(
                post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new OrderRequest.Place(
                                seller.getId(),
                                market.getId(),
                                "ask-idempotency-1",
                                OrderSide.ASK,
                                OrderType.LIMIT,
                                null,
                                ORDER_PRICE,
                                new BigDecimal("0.3"),
                                null
                        )))
        ).andExpect(status().isConflict()).andReturn());

        assertThat(failure.path("code").asString()).isEqualTo("D13");
    }

    @Test
    @DisplayName("Negative : 잘못된 MARKET-BID 요청은 business validation error를 반환한다.")
    void place_invalid_market_bid_request_returns_business_error() throws Exception {
        JsonNode failure = failureRoot(mockMvc.perform(
                post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new OrderRequest.Place(
                                buyer.getId(),
                                market.getId(),
                                "invalid-market-bid-1",
                                OrderSide.BID,
                                OrderType.MARKET,
                                null,
                                ORDER_PRICE,
                                null,
                                null
                        )))
        ).andExpect(status().isBadRequest()).andReturn());

        assertThat(failure.path("code").asString()).isEqualTo("D05");
    }

    @Test
    @DisplayName("Negative : 존재하지 않는 시장의 호가 조회는 MARKET_NOT_FOUND를 반환한다.")
    void find_order_book_with_unknown_market_returns_market_not_found() throws Exception {
        JsonNode failure = failureRoot(mockMvc.perform(
                get("/api/v1/order-books")
                        .param("marketId", "999999")
        ).andExpect(status().isNotFound()).andReturn());

        assertThat(failure.path("code").asString()).isEqualTo("C03");
    }

    @Test
    @DisplayName("Negative : 존재하지 않는 주문 상세 조회는 ORDER_NOT_FOUND를 반환한다.")
    void find_order_detail_with_unknown_client_order_id_returns_order_not_found() throws Exception {
        JsonNode failure = failureRoot(mockMvc.perform(
                get("/api/v1/orders/detail")
                        .param("userId", seller.getId().toString())
                        .param("clientOrderId", "unknown-order")
        ).andExpect(status().isNotFound()).andReturn());

        assertThat(failure.path("code").asString()).isEqualTo("D11");
    }

    @Test
    @DisplayName("Negative : 다른 사용자의 walletId로 원장 조회하면 WALLET_NOT_FOUND를 반환한다.")
    void find_ledgers_with_wallet_ownership_mismatch_returns_wallet_not_found() throws Exception {
        JsonNode failure = failureRoot(mockMvc.perform(
                get("/api/v1/ledgers")
                        .param("userId", buyer.getId().toString())
                        .param("walletId", sellerBtcWallet.getId().toString())
        ).andExpect(status().isNotFound()).andReturn());

        assertThat(failure.path("code").asString()).isEqualTo("G06");
    }

    private Asset persistAsset(String symbol, String name, byte decimals) {
        Asset asset = Asset.create(symbol, name, decimals, EnumStatus.ACTIVE);
        entityManager.persist(asset);
        entityManager.flush();
        return asset;
    }

    private MvcResult placeLimitAsk(String clientOrderId) throws Exception {
        return placeOrder(new OrderRequest.Place(
                seller.getId(),
                market.getId(),
                clientOrderId,
                OrderSide.ASK,
                OrderType.LIMIT,
                null,
                ORDER_PRICE,
                ORDER_QUANTITY,
                null
        ));
    }

    private MvcResult placeLimitBid(String clientOrderId) throws Exception {
        return placeOrder(new OrderRequest.Place(
                buyer.getId(),
                market.getId(),
                clientOrderId,
                OrderSide.BID,
                OrderType.LIMIT,
                null,
                ORDER_PRICE,
                ORDER_QUANTITY,
                null
        ));
    }

    private MvcResult placeMarketBid(String clientOrderId, BigDecimal quoteAmount) throws Exception {
        return placeOrder(new OrderRequest.Place(
                buyer.getId(),
                market.getId(),
                clientOrderId,
                OrderSide.BID,
                OrderType.MARKET,
                null,
                null,
                null,
                quoteAmount
        ));
    }

    private MvcResult placeMarketAsk(String clientOrderId, BigDecimal quantity) throws Exception {
        return placeOrder(new OrderRequest.Place(
                seller.getId(),
                market.getId(),
                clientOrderId,
                OrderSide.ASK,
                OrderType.MARKET,
                null,
                null,
                quantity,
                null
        ));
    }

    private MvcResult placeOrder(OrderRequest.Place request) throws Exception {
        return mockMvc.perform(
                post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
        ).andExpect(status().isAccepted()).andReturn();
    }

    private JsonNode successData(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.path("success").asBoolean()).isTrue();
        assertThat(root.path("code").asString()).isEqualTo("SUCCESS");
        return root.path("data");
    }

    private JsonNode failureRoot(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.path("success").asBoolean()).isFalse();
        return root;
    }

    private String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private void awaitOrderStatus(Long userId, String clientOrderId, OrderStatus expectedStatus) {
        awaitCondition(
                "order status = " + expectedStatus,
                Duration.ofSeconds(5),
                () -> orderRepository.findByUserIdAndClientOrderId(userId, clientOrderId)
                        .map(Order::getStatus)
                        .filter(expectedStatus::equals)
                        .isPresent()
        );
    }

    private void awaitTradeCount(Long marketId, int expectedCount) {
        awaitCondition(
                "trade count = " + expectedCount,
                Duration.ofSeconds(5),
                () -> tradeRepository.findTop100ByMarketIdOrderByIdDesc(marketId).size() == expectedCount
        );
    }

    private JsonNode awaitOrderBookLevelCounts(int expectedBidLevels, int expectedAskLevels) {
        JsonNode[] resultHolder = new JsonNode[1];
        awaitCondition(
                "order book levels = bids " + expectedBidLevels + ", asks " + expectedAskLevels,
                Duration.ofSeconds(5),
                () -> {
                    try {
                        JsonNode orderBook = successData(mockMvc.perform(
                                get("/api/v1/order-books")
                                        .param("marketId", market.getId().toString())
                        ).andExpect(status().isOk()).andReturn());
                        resultHolder[0] = orderBook;
                        return orderBook.path("bids").size() == expectedBidLevels
                                && orderBook.path("asks").size() == expectedAskLevels;
                    } catch (Exception exception) {
                        throw new IllegalStateException("호가 조회 대기 중 실패", exception);
                    }
                }
        );
        return resultHolder[0];
    }

    private void awaitCondition(String description, Duration timeout, Condition condition) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.matches()) {
                return;
            }
            sleep();
        }
        fail("타임아웃: " + description);
    }

    private void sleep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("대기 중 인터럽트가 발생했습니다.", exception);
        }
    }

    @FunctionalInterface
    private interface Condition {
        boolean matches();
    }

    private record SeedData(
            User seller,
            User buyer,
            Asset btcAsset,
            Asset krwAsset,
            Market market,
            Wallet sellerBtcWallet,
            Wallet sellerKrwWallet,
            Wallet buyerBtcWallet,
            Wallet buyerKrwWallet
    ) {
    }
}
