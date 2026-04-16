package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.application.service.LedgerWriteService;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.wallet.domain.model.Ledger;
import com.project.upbit_clone.wallet.domain.model.Wallet;
import com.project.upbit_clone.wallet.domain.repository.LedgerRepository;
import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import com.project.upbit_clone.wallet.domain.vo.ChangeType;
import com.project.upbit_clone.wallet.domain.vo.LedgerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderWriteService 단위 테스트")
class OrderWriteServiceTest {

    @Mock
    private CommandLogRepository commandLogRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Captor
    private ArgumentCaptor<Ledger> ledgerCaptor;

    private OrderWriteService orderWriteService;

    @BeforeEach
    void setUp() {
        orderWriteService = new OrderWriteService(
                commandLogRepository,
                orderRepository,
                walletRepository,
                new LedgerWriteService(ledgerRepository)
        );
    }

    @Test
    @DisplayName("Happy : acceptance write는 ORDER_LOCK ledger를 함께 저장한다.")
    void writeAcceptedPlace_persists_order_lock_ledger() {
        CommandLog commandLog = commandLog();
        User user = user();
        setField(user, "id", 1L);
        Market market = market();
        setField(market, "id", 1L);
        setField(market.getBaseAsset(), "id", 10L);
        setField(market.getQuoteAsset(), "id", 20L);
        Wallet quoteWallet = Wallet.create(user, market.getQuoteAsset(), new BigDecimal("10000"), BigDecimal.ZERO);
        setField(quoteWallet, "id", 100L);

        OrderWriteService.AcceptedPlaceCommand command = new OrderWriteService.AcceptedPlaceCommand(
                commandLog,
                user,
                market,
                "cid-1",
                "order-key-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("10000"),
                BigDecimal.ONE,
                null
        );

        when(walletRepository.findByUserIdAndAssetId(user.getId(), market.getQuoteAsset().getId()))
                .thenReturn(Optional.of(quoteWallet));
        when(commandLogRepository.saveAndFlush(any(CommandLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.saveAndFlush(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order savedOrder = invocation.getArgument(0);
                    setField(savedOrder, "id", 200L);
                    return savedOrder;
                });
        when(ledgerRepository.save(any(Ledger.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderWriteService.AcceptedPlaceWrite result = orderWriteService.writeAcceptedPlace(command);

        assertThat(result.wallet().getAvailableBalance()).isEqualByComparingTo("0");
        assertThat(result.wallet().getLockedBalance()).isEqualByComparingTo("10000");
        assertThat(result.order().getId()).isEqualTo(200L);

        verify(ledgerRepository).save(ledgerCaptor.capture());
        Ledger ledger = ledgerCaptor.getValue();
        assertThat(ledger.getLedgerType()).isEqualTo(LedgerType.ORDER_LOCK);
        assertThat(ledger.getChangeType()).isEqualTo(ChangeType.DECREASE);
        assertThat(ledger.getAmount()).isEqualByComparingTo("10000");
        assertThat(ledger.getAvailableBefore()).isEqualByComparingTo("10000");
        assertThat(ledger.getAvailableAfter()).isEqualByComparingTo("0");
        assertThat(ledger.getLockedBefore()).isEqualByComparingTo("0");
        assertThat(ledger.getLockedAfter()).isEqualByComparingTo("10000");
        assertThat(ledger.getReferenceId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("Happy : LIMIT-BID lock 금액은 quote asset scale 기준으로 내림 처리한다.")
    void writeAcceptedPlace_rounds_limit_bid_lock_amount_down_by_quote_asset_scale() {
        CommandLog commandLog = commandLog();
        User user = user();
        setField(user, "id", 1L);
        Market market = market((byte) 2);
        setField(market, "id", 1L);
        setField(market.getBaseAsset(), "id", 10L);
        setField(market.getQuoteAsset(), "id", 20L);
        Wallet quoteWallet = Wallet.create(user, market.getQuoteAsset(), new BigDecimal("20000"), BigDecimal.ZERO);
        setField(quoteWallet, "id", 100L);

        OrderWriteService.AcceptedPlaceCommand command = new OrderWriteService.AcceptedPlaceCommand(
                commandLog,
                user,
                market,
                "cid-1",
                "order-key-1",
                OrderSide.BID,
                OrderType.LIMIT,
                TimeInForce.GTC,
                new BigDecimal("10000"),
                new BigDecimal("1.23456789"),
                null
        );

        when(walletRepository.findByUserIdAndAssetId(user.getId(), market.getQuoteAsset().getId()))
                .thenReturn(Optional.of(quoteWallet));
        when(commandLogRepository.saveAndFlush(any(CommandLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.saveAndFlush(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order savedOrder = invocation.getArgument(0);
                    setField(savedOrder, "id", 200L);
                    return savedOrder;
                });
        when(ledgerRepository.save(any(Ledger.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderWriteService.AcceptedPlaceWrite result = orderWriteService.writeAcceptedPlace(command);

        assertThat(result.wallet().getAvailableBalance()).isEqualByComparingTo("7654.33");
        assertThat(result.wallet().getLockedBalance()).isEqualByComparingTo("12345.67");

        verify(ledgerRepository).save(ledgerCaptor.capture());
        Ledger ledger = ledgerCaptor.getValue();
        assertThat(ledger.getAmount()).isEqualByComparingTo("12345.67");
        assertThat(ledger.getAvailableBefore()).isEqualByComparingTo("20000");
        assertThat(ledger.getAvailableAfter()).isEqualByComparingTo("7654.33");
        assertThat(ledger.getLockedBefore()).isEqualByComparingTo("0");
        assertThat(ledger.getLockedAfter()).isEqualByComparingTo("12345.67");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidAcceptedPlaceCommands")
    @DisplayName("Negative : 주문 정책 위반이면 command log 저장과 wallet 조회 전에 실패한다.")
    void writeAcceptedPlace_rejects_invalid_order_before_persistence_and_wallet_access(
            String caseName,
            OrderWriteService.AcceptedPlaceCommand command,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(() -> orderWriteService.writeAcceptedPlace(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);

        verifyNoInteractions(commandLogRepository, orderRepository, walletRepository);
    }

    private static Stream<Arguments> invalidAcceptedPlaceCommands() {
        return Stream.of(
                Arguments.of(
                        "tick size 위반 지정가 매수",
                        acceptedPlaceCommand(
                                OrderSide.BID,
                                OrderType.LIMIT,
                                TimeInForce.GTC,
                                new BigDecimal("10500"),
                                BigDecimal.ONE,
                                null
                        ),
                        ErrorCode.INVALID_LIMIT_BID_INPUT
                ),
                Arguments.of(
                        "최소 주문 금액 미달 시장가 매수",
                        acceptedPlaceCommand(
                                OrderSide.BID,
                                OrderType.MARKET,
                                TimeInForce.IOC,
                                null,
                                null,
                                new BigDecimal("4000")
                        ),
                        ErrorCode.INVALID_MARKET_BID_INPUT
                ),
                Arguments.of(
                        "허용 scale 초과 지정가 매도",
                        acceptedPlaceCommand(
                                OrderSide.ASK,
                                OrderType.LIMIT,
                                TimeInForce.GTC,
                                new BigDecimal("10000"),
                                new BigDecimal("1.123456789"),
                                null
                        ),
                        ErrorCode.INVALID_LIMIT_ASK_INPUT
                )
        );
    }

    private static OrderWriteService.AcceptedPlaceCommand acceptedPlaceCommand(
            OrderSide orderSide,
            OrderType orderType,
            TimeInForce timeInForce,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount
    ) {
        return new OrderWriteService.AcceptedPlaceCommand(
                commandLog(),
                user(),
                market(),
                "cid-1",
                "order-key-1",
                orderSide,
                orderType,
                timeInForce,
                price,
                quantity,
                quoteAmount
        );
    }

    private static CommandLog commandLog() {
        return CommandLog.create(new CommandLog.CreateCommand(
                "command-id-1",
                CommandType.PLACE_ORDER,
                1L,
                1L,
                "cid-1",
                "{\"type\":\"place\"}",
                "request-hash-1"
        ));
    }

    private static User user() {
        return User.create("u@test.com", "user", EnumStatus.ACTIVE, "pw");
    }

    private static Market market() {
        return market((byte) 8);
    }

    private static Market market(byte quoteAssetDecimals) {
        Asset baseAsset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Asset quoteAsset = Asset.create("KRW", "Korean Won", quoteAssetDecimals, EnumStatus.ACTIVE);
        return Market.create(new Market.CreateCommand(
                baseAsset,
                quoteAsset,
                EnumStatus.ACTIVE,
                new BigDecimal("5000"),
                new BigDecimal("1000")
        ));
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
