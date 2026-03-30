package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.application.dispatch.CommandDispatcher;
import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceOrder 단위 테스트")
class PlaceOrderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private IdempotencyHitService idempotencyHitService;

    @Mock
    private CommandLogAppendService commandLogAppendService;

    @Mock
    private CommandDispatcher commandDispatcher;

    private PlaceOrder placeOrder;

    @BeforeEach
    void setUp() {
        placeOrder = new PlaceOrder(
                userRepository,
                marketRepository,
                JsonMapper.builder().build(),
                idempotencyHitService,
                commandLogAppendService,
                new OrderCommandHashService(),
                commandDispatcher
        );
    }

    @Test
    @DisplayName("Happy : 유효한 요청이면 ACCEPTED 응답을 반환한다.")
    void handle_with_valid_command() {
        // given
        PlaceOrder.Command command = validCommand();
        User activeUser = User.create("u@test.com", "user", EnumStatus.ACTIVE, "pw");
        Market activeMarket = activeMarket();

        when(idempotencyHitService.find(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(marketRepository.findWithAssetsById(1L)).thenReturn(Optional.of(activeMarket));
        when(commandLogAppendService.append(any(CommandLog.class)))
                .thenAnswer(invocation -> {
                    CommandLog log = invocation.getArgument(0);
                    setCommandLogId(log, 100L);
                    return log;
                });

        // when
        CommandAck ack = placeOrder.handle(command);

        // then
        assertThat(ack.status()).isEqualTo(CommandAck.Status.ACCEPTED);
        assertThat(ack.idempotencyHit()).isFalse();
        assertThat(ack.commandType()).isEqualTo(CommandType.PLACE_ORDER);
        assertThat(ack.commandLogId()).isEqualTo(100L);

        ArgumentCaptor<CommandLog> captor = ArgumentCaptor.forClass(CommandLog.class);
        verify(commandLogAppendService).append(captor.capture());
        CommandLog appended = captor.getValue();
        assertThat(appended.getUserId()).isEqualTo(1L);
        assertThat(appended.getMarketId()).isEqualTo(1L);
        assertThat(appended.getClientOrderId()).isEqualTo("cid-1");
        verify(commandDispatcher).dispatch(any(CommandMessage.class));
    }

    @Test
    @DisplayName("Happy : dispatch 실패여도 append 성공이면 ACCEPTED 응답을 반환한다.")
    void handle_returns_accepted_when_dispatch_fails() {
        // given
        PlaceOrder.Command command = validCommand();
        User activeUser = User.create("u@test.com", "user", EnumStatus.ACTIVE, "pw");
        Market activeMarket = activeMarket();

        when(idempotencyHitService.find(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(marketRepository.findWithAssetsById(1L)).thenReturn(Optional.of(activeMarket));
        when(commandLogAppendService.append(any(CommandLog.class)))
                .thenAnswer(invocation -> {
                    CommandLog log = invocation.getArgument(0);
                    setCommandLogId(log, 100L);
                    return log;
                });
        doThrow(new RuntimeException("dispatch failed"))
                .when(commandDispatcher).dispatch(any(CommandMessage.class));

        // when
        CommandAck ack = placeOrder.handle(command);

        // then
        assertThat(ack.status()).isEqualTo(CommandAck.Status.ACCEPTED);
        assertThat(ack.idempotencyHit()).isFalse();
        assertThat(ack.commandLogId()).isEqualTo(100L);
        verify(commandDispatcher).dispatch(any(CommandMessage.class));
    }

    @Test
    @DisplayName("Happy : idempotency hit이면 기존 로그로 ACCEPTED(idempotencyHit=true)를 반환한다.")
    void handle_with_idempotency_hit() {
        // given
        PlaceOrder.Command command = validCommand();
        String requestHash = new OrderCommandHashService().hash(command);
        CommandLog existing = CommandLog.create(new CommandLog.CreateCommand(
                "existing-cmd",
                CommandType.PLACE_ORDER,
                1L,
                1L,
                "cid-1",
                "{\"order\":\"place\"}",
                requestHash
        ));
        setCommandLogId(existing, 555L);

        when(idempotencyHitService.find(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.of(existing));

        // when
        CommandAck ack = placeOrder.handle(command);

        // then
        assertThat(ack.status()).isEqualTo(CommandAck.Status.ACCEPTED);
        assertThat(ack.idempotencyHit()).isTrue();
        assertThat(ack.commandId()).isEqualTo("existing-cmd");
        assertThat(ack.commandLogId()).isEqualTo(555L);
        assertThat(ack.commandType()).isEqualTo(CommandType.PLACE_ORDER);
        verify(userRepository, never()).findById(any());
        verify(marketRepository, never()).findWithAssetsById(any());
        verify(commandLogAppendService, never()).append(any(CommandLog.class));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("missingRequiredInputs")
    @DisplayName("Negative : 필수 입력값 누락이면 BusinessException을 반환한다.")
    void handle_with_missing_required_value(String caseName, PlaceOrder.Command command) {
        // when & then
        assertThatThrownBy(() -> placeOrder.handle(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MISSING_ORDER_REQUIRED_VALUE);
    }

    private static Stream<Arguments> missingRequiredInputs() {
        return Stream.of(
                Arguments.of("command null", null),
                Arguments.of("userId null", new PlaceOrder.Command(
                        null, 1L, "cid-1", OrderSide.BID, OrderType.LIMIT,
                        TimeInForce.GTC, new BigDecimal("10000"), BigDecimal.ONE, null
                )),
                Arguments.of("marketId null", new PlaceOrder.Command(
                        1L, null, "cid-1", OrderSide.BID, OrderType.LIMIT,
                        TimeInForce.GTC, new BigDecimal("10000"), BigDecimal.ONE, null
                )),
                Arguments.of("clientOrderId null", new PlaceOrder.Command(
                        1L, 1L, null, OrderSide.BID, OrderType.LIMIT,
                        TimeInForce.GTC, new BigDecimal("10000"), BigDecimal.ONE, null
                )),
                Arguments.of("clientOrderId blank", new PlaceOrder.Command(
                        1L, 1L, "   ", OrderSide.BID, OrderType.LIMIT,
                        TimeInForce.GTC, new BigDecimal("10000"), BigDecimal.ONE, null
                ))
        );
    }

    @Test
    @DisplayName("Negative : 사용자 조회 실패 시 BusinessException을 반환한다.")
    void handle_with_user_not_found() {
        // given
        PlaceOrder.Command command = validCommand();
        when(idempotencyHitService.find(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> placeOrder.handle(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Negative : 사용자 상태가 ACTIVE가 아니면 BusinessException을 반환한다.")
    void handle_with_inactive_user() {
        // given
        PlaceOrder.Command command = validCommand();
        User inactiveUser = User.create("u@test.com", "user", EnumStatus.INACTIVE, "pw");
        when(idempotencyHitService.find(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(inactiveUser));

        // when & then
        assertThatThrownBy(() -> placeOrder.handle(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ALLOWED_USER_STATUS);
    }

    @Test
    @DisplayName("Negative : 시장 조회 실패 시 BusinessException을 반환한다.")
    void handle_with_market_not_found() {
        // given
        PlaceOrder.Command command = validCommand();
        User activeUser = User.create("u@test.com", "user", EnumStatus.ACTIVE, "pw");
        when(idempotencyHitService.find(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(marketRepository.findWithAssetsById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> placeOrder.handle(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MARKET_NOT_FOUND);
    }

    @Test
    @DisplayName("Negative : 시장 상태가 ACTIVE가 아니면 BusinessException을 반환한다.")
    void handle_with_inactive_market() {
        // given
        PlaceOrder.Command command = validCommand();
        User activeUser = User.create("u@test.com", "user", EnumStatus.ACTIVE, "pw");
        Market inactiveMarket = inactiveMarket();
        when(idempotencyHitService.find(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(marketRepository.findWithAssetsById(1L)).thenReturn(Optional.of(inactiveMarket));

        // when & then
        assertThatThrownBy(() -> placeOrder.handle(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ALLOWED_MARKET_STATUS);
    }

    @Test
    @DisplayName("Negative : 주문 정책이 유효하지 않으면 BusinessException을 반환한다.")
    void handle_with_invalid_order_policy() {
        // given
        PlaceOrder.Command invalid = new PlaceOrder.Command(
                1L, 1L, "cid-1",
                OrderSide.BID, OrderType.LIMIT, TimeInForce.GTC,
                new BigDecimal("10000"), null, null
        );
        User activeUser = User.create("u@test.com", "user", EnumStatus.ACTIVE, "pw");
        Market activeMarket = activeMarket();
        when(idempotencyHitService.find(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(marketRepository.findWithAssetsById(1L)).thenReturn(Optional.of(activeMarket));

        // when & then
        assertThatThrownBy(() -> placeOrder.handle(invalid))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LIMIT_BID_INPUT);
        verify(commandLogAppendService, never()).append(any(CommandLog.class));
    }

    private PlaceOrder.Command validCommand() {
        return new PlaceOrder.Command(
                1L, 1L, "cid-1",
                OrderSide.BID, OrderType.LIMIT, TimeInForce.GTC,
                new BigDecimal("10000"), BigDecimal.ONE, null
        );
    }

    private Market activeMarket() {
        return createMarket(EnumStatus.ACTIVE);
    }

    private Market inactiveMarket() {
        return createMarket(EnumStatus.INACTIVE);
    }

    private Market createMarket(EnumStatus status) {
        Asset base = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Asset quote = Asset.create("KRW", "Korean Won", (byte) 2, EnumStatus.ACTIVE);
        return Market.create(new Market.CreateCommand(
                base,
                quote,
                status,
                new BigDecimal("5000"),
                new BigDecimal("1000")
        ));
    }

    private static void setCommandLogId(CommandLog commandLog, Long id) {
        try {
            Field field = CommandLog.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(commandLog, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("CommandLog id 필드 주입 실패", e);
        }
    }
}
