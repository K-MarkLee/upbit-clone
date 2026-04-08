package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.EngineResult;
import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.application.engine.orderbook.PriceLevel;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.model.Trade;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.repository.TradeRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffset;
import com.project.upbit_clone.trade.infrastructure.persistence.model.EventLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.ConsumerOffsetRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.EventLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.EventType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.wallet.domain.model.Wallet;
import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerWriteService 단위 테스트")
class WorkerWriteServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private CommandLogRepository commandLogRepository;

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private ConsumerOffsetRepository consumerOffsetRepository;

    @Captor
    private ArgumentCaptor<Iterable<Wallet>> walletCaptor;

    @Captor
    private ArgumentCaptor<Iterable<Trade>> tradeCaptor;

    @Captor
    private ArgumentCaptor<Iterable<EventLog>> eventCaptor;

    @Captor
    private ArgumentCaptor<ConsumerOffset> offsetCaptor;

    private WorkerWriteService workerWriteService;

    @BeforeEach
    void setUp() {
        workerWriteService = new WorkerWriteService(
                orderRepository,
                walletRepository,
                tradeRepository,
                commandLogRepository,
                eventLogRepository,
                consumerOffsetRepository,
                JsonMapper.builder().build()
        );
    }

    @Test
    @DisplayName("Happy : OPEN no-fill 결과면 taker를 OPEN으로 확정하고 offset/event를 저장한다.")
    void writePlace_marks_taker_open_without_trade() {
        User takerUser = user(1L, "taker@test.com");
        Market market = market(100L);
        Order taker = pendingOrder(10L, market, takerUser, "taker-order", OrderSide.BID, new BigDecimal("10000"), BigDecimal.ONE);
        Wallet takerQuoteWallet = wallet(1000L, takerUser, market.getQuoteAsset(), BigDecimal.ZERO, new BigDecimal("10000"));
        CommandLog commandLog = commandLog(11L, 100L);

        CommandMessage.Place message = new CommandMessage.Place(
                11L,
                takerUser.getId(),
                100L,
                "KRW-BTC",
                "cid-1",
                taker.getOrderKey(),
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("10000"),
                BigDecimal.ONE,
                null,
                8
        );
        EngineResult.PlaceResult result = EngineResult.PlaceResult.open(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                List.of(),
                List.of()
        );

        when(orderRepository.findByOrderKey(taker.getOrderKey())).thenReturn(Optional.of(taker));
        when(walletRepository.findAllByUserIdInAndAssetIdIn(any(), any())).thenReturn(List.of(takerQuoteWallet));
        when(commandLogRepository.getReferenceById(11L)).thenReturn(commandLog);

        workerWriteService.writePlace(message, result);

        assertThat(taker.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(takerQuoteWallet.getLockedBalance()).isEqualByComparingTo("10000");
        assertThat(takerQuoteWallet.getAvailableBalance()).isEqualByComparingTo("0");

        verify(tradeRepository, never()).saveAll(any());

        verify(eventLogRepository).saveAll(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .extracting(EventLog::getEventType)
                .containsExactly(EventType.ORDER_OPENED);

        verify(consumerOffsetRepository).save(offsetCaptor.capture());
        assertThat(offsetCaptor.getValue().getLastOffset()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Happy : FILLED 결과면 trade 생성, wallet 정산, unlock, event 저장을 수행한다.")
    void writePlace_persists_trade_and_wallet_settlement_for_filled_bid() {
        User takerUser = user(1L, "taker@test.com");
        User makerUser = user(2L, "maker@test.com");
        Market market = market(100L);
        Order taker = pendingOrder(10L, market, takerUser, "taker-order", OrderSide.BID, new BigDecimal("10000"), BigDecimal.ONE);
        Order maker = openOrder(20L, market, makerUser, "maker-order", OrderSide.ASK, new BigDecimal("9000"), BigDecimal.ONE);
        Wallet takerQuoteWallet = wallet(1000L, takerUser, market.getQuoteAsset(), BigDecimal.ZERO, new BigDecimal("10000"));
        Wallet makerBaseWallet = wallet(2000L, makerUser, market.getBaseAsset(), BigDecimal.ZERO, BigDecimal.ONE);
        CommandLog commandLog = commandLog(11L, 100L);

        CommandMessage.Place message = new CommandMessage.Place(
                11L,
                takerUser.getId(),
                100L,
                "KRW-BTC",
                "cid-1",
                taker.getOrderKey(),
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("10000"),
                BigDecimal.ONE,
                null,
                8
        );
        EngineResult.PlaceResult result = EngineResult.PlaceResult.filled(
                BigDecimal.ONE,
                new BigDecimal("9000"),
                new BigDecimal("1000"),
                List.of(new EngineResult.Fill(
                        maker.getOrderKey(),
                        new BigDecimal("9000"),
                        BigDecimal.ONE,
                        new BigDecimal("9000"),
                        BigDecimal.ZERO
                )),
                List.of()
        );

        when(orderRepository.findByOrderKey(taker.getOrderKey())).thenReturn(Optional.of(taker));
        when(orderRepository.findAllByOrderKeyIn(List.of(maker.getOrderKey()))).thenReturn(List.of(maker));
        when(walletRepository.findAllByUserIdInAndAssetIdIn(any(), any())).thenReturn(List.of(takerQuoteWallet, makerBaseWallet));
        when(commandLogRepository.getReferenceById(11L)).thenReturn(commandLog);

        workerWriteService.writePlace(message, result);

        assertThat(taker.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(maker.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(taker.getExecutedQuantity()).isEqualByComparingTo("1");
        assertThat(taker.getExecutedQuoteAmount()).isEqualByComparingTo("9000");
        assertThat(maker.getExecutedQuantity()).isEqualByComparingTo("1");
        assertThat(maker.getExecutedQuoteAmount()).isEqualByComparingTo("9000");
        assertThat(takerQuoteWallet.getAvailableBalance()).isEqualByComparingTo("1000");
        assertThat(takerQuoteWallet.getLockedBalance()).isEqualByComparingTo("0");
        assertThat(makerBaseWallet.getLockedBalance()).isEqualByComparingTo("0");

        verify(walletRepository).saveAll(walletCaptor.capture());
        assertThat(walletCaptor.getValue()).hasSize(4);

        verify(tradeRepository).saveAll(tradeCaptor.capture());
        assertThat(tradeCaptor.getValue()).hasSize(1);
        Trade trade = tradeCaptor.getValue().iterator().next();
        assertThat(trade.getTradeKey()).isEqualTo("taker-order-trade-1");
        assertThat(trade.getPrice()).isEqualByComparingTo("9000");

        verify(eventLogRepository).saveAll(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .extracting(EventLog::getEventType)
                .containsExactly(EventType.TRADE_EXECUTED, EventType.ORDER_FILLED, EventType.FUNDS_UNLOCKED);
    }

    @Test
    @DisplayName("Happy : OPEN 주문 cancel이면 CANCELED, unlock, order book delta, offset를 저장한다.")
    void writeCancel_cancels_open_order_and_unlocks_funds() {
        User user = user(1L, "user@test.com");
        Market market = market(100L);
        Order order = openOrder(10L, market, user, "open-order", OrderSide.BID, new BigDecimal("10000"), BigDecimal.ONE);
        Wallet quoteWallet = wallet(1000L, user, market.getQuoteAsset(), BigDecimal.ZERO, new BigDecimal("10000"));
        CommandLog commandLog = commandLog(21L, 100L, CommandType.CANCEL_ORDER);
        InMemoryOrderBook.LevelDelta removedLevelDelta = levelDelta(
                OrderSide.BID,
                new BigDecimal("10000"),
                BigDecimal.ONE,
                1,
                BigDecimal.ZERO,
                0
        );

        CommandMessage.Cancel message = new CommandMessage.Cancel(
                21L,
                user.getId(),
                100L,
                "KRW-BTC",
                "cid-1",
                order.getOrderKey(),
                "USER_REQUEST"
        );

        when(orderRepository.findByOrderKey(order.getOrderKey())).thenReturn(Optional.of(order));
        when(walletRepository.findAllByUserIdInAndAssetIdIn(any(), any())).thenReturn(List.of(quoteWallet));
        when(commandLogRepository.getReferenceById(21L)).thenReturn(commandLog);

        workerWriteService.writeCancel(message, removedLevelDelta);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCancelReason()).isEqualTo("USER_REQUEST");
        assertThat(quoteWallet.getAvailableBalance()).isEqualByComparingTo("10000");
        assertThat(quoteWallet.getLockedBalance()).isEqualByComparingTo("0");

        verify(tradeRepository, never()).saveAll(any());

        verify(eventLogRepository).saveAll(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .extracting(EventLog::getEventType)
                .containsExactly(EventType.ORDER_CANCELED, EventType.FUNDS_UNLOCKED, EventType.ORDER_BOOK_DELTA);

        verify(consumerOffsetRepository).save(offsetCaptor.capture());
        assertThat(offsetCaptor.getValue().getLastOffset()).isEqualTo(21L);
    }

    @Test
    @DisplayName("Happy : PENDING 주문 cancel이면 CANCELED와 unlock을 저장하고 order book delta는 남기지 않는다.")
    void writeCancel_cancels_pending_order_without_book_delta() {
        User user = user(1L, "user@test.com");
        Market market = market(100L);
        Order order = pendingOrder(10L, market, user, "pending-order", OrderSide.ASK, new BigDecimal("10000"), new BigDecimal("2"));
        Wallet baseWallet = wallet(1000L, user, market.getBaseAsset(), BigDecimal.ZERO, new BigDecimal("2"));
        CommandLog commandLog = commandLog(31L, 100L, CommandType.CANCEL_ORDER);

        CommandMessage.Cancel message = new CommandMessage.Cancel(
                31L,
                user.getId(),
                100L,
                "KRW-BTC",
                "cid-2",
                order.getOrderKey(),
                null
        );

        when(orderRepository.findByOrderKey(order.getOrderKey())).thenReturn(Optional.of(order));
        when(walletRepository.findAllByUserIdInAndAssetIdIn(any(), any())).thenReturn(List.of(baseWallet));
        when(commandLogRepository.getReferenceById(31L)).thenReturn(commandLog);

        workerWriteService.writeCancel(message, null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCancelReason()).isEqualTo("Order Canceled");
        assertThat(baseWallet.getAvailableBalance()).isEqualByComparingTo("2");
        assertThat(baseWallet.getLockedBalance()).isEqualByComparingTo("0");

        verify(eventLogRepository).saveAll(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .extracting(EventLog::getEventType)
                .containsExactly(EventType.ORDER_CANCELED, EventType.FUNDS_UNLOCKED);
    }

    private static User user(Long id, String email) {
        User user = User.create(email, email, EnumStatus.ACTIVE, "pw");
        setField(user, "id", id);
        return user;
    }

    private static Market market(Long id) {
        Asset baseAsset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Asset quoteAsset = Asset.create("KRW", "Korean Won", (byte) 8, EnumStatus.ACTIVE);
        setField(baseAsset, "id", 1L);
        setField(quoteAsset, "id", 2L);

        Market market = Market.create(new Market.CreateCommand(
                baseAsset,
                quoteAsset,
                EnumStatus.ACTIVE,
                new BigDecimal("5000"),
                new BigDecimal("1000")
        ));
        setField(market, "id", id);
        return market;
    }

    private static Order pendingOrder(
            Long id,
            Market market,
            User user,
            String orderKey,
            OrderSide side,
            BigDecimal price,
            BigDecimal quantity
    ) {
        Order order = Order.create(new Order.CreateCommand(
                market,
                user,
                orderKey + "-cid",
                orderKey,
                side,
                OrderType.LIMIT,
                TimeInForce.GTC,
                price,
                quantity,
                null
        ));
        setField(order, "id", id);
        return order;
    }

    private static Order openOrder(
            Long id,
            Market market,
            User user,
            String orderKey,
            OrderSide side,
            BigDecimal price,
            BigDecimal quantity
    ) {
        Order order = pendingOrder(id, market, user, orderKey, side, price, quantity);
        order.markOpen();
        return order;
    }

    private static Wallet wallet(Long id, User user, Asset asset, BigDecimal available, BigDecimal locked) {
        Wallet wallet = Wallet.create(user, asset, available, locked);
        setField(wallet, "id", id);
        return wallet;
    }

    private static CommandLog commandLog(Long id, Long marketId) {
        return commandLog(id, marketId, CommandType.PLACE_ORDER);
    }

    private static CommandLog commandLog(Long id, Long marketId, CommandType commandType) {
        CommandLog commandLog = CommandLog.create(new CommandLog.CreateCommand(
                "command-id-" + id,
                commandType,
                marketId,
                1L,
                "cid-1",
                "{\"type\":\"place\"}",
                "request-hash-" + id
        ));
        setField(commandLog, "id", id);
        return commandLog;
    }

    private static InMemoryOrderBook.LevelDelta levelDelta(
            OrderSide side,
            BigDecimal price,
            BigDecimal beforeQty,
            int beforeOrderCount,
            BigDecimal afterQty,
            int afterOrderCount
    ) {
        return new InMemoryOrderBook.LevelDelta(
                side,
                price,
                new PriceLevel.Snapshot(side, price, beforeQty, beforeOrderCount),
                new PriceLevel.Snapshot(side, price, afterQty, afterOrderCount)
        );
    }

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("필드 설정 실패: " + fieldName, exception);
            }
        }
        throw new IllegalStateException("필드를 찾을 수 없습니다: " + fieldName);
    }
}
