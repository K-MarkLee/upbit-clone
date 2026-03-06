package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelOrder 단위 테스트")
class CancelOrderTest {

    @Mock
    private CommandLogRepository commandLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private IdempotencyHitService idempotencyHitService;

    @Mock
    private CommandLogAppendService commandLogAppendService;

    private CancelOrder cancelOrder;

    @BeforeEach
    void setUp() {
        cancelOrder = new CancelOrder(
                commandLogRepository,
                userRepository,
                marketRepository,
                JsonMapper.builder().build(),
                idempotencyHitService,
                commandLogAppendService,
                new OrderCommandHashService()
        );
    }

    @Test
    @DisplayName("Happy : 유효한 취소 요청이면 ACCEPTED 응답을 반환한다.")
    void handle_with_valid_command() {
        // given
        CancelOrder.Command command = validCommand();
        User activeUser = User.create("u@test.com", "user", EnumStatus.ACTIVE, "pw");
        Market activeMarket = createMarket(EnumStatus.ACTIVE);
        CommandLog placeLog = CommandLog.create(new CommandLog.CreateCommand(
                "place-1",
                CommandType.PLACE_ORDER,
                1L,
                1L,
                "cid-1",
                "{\"order\":\"place\"}",
                "hash-place"
        ));

        when(idempotencyHitService.find(1L, "cid-1", CommandType.CANCEL_ORDER))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(marketRepository.findWithAssetsById(1L)).thenReturn(Optional.of(activeMarket));
        when(commandLogRepository.findByUserIdAndClientOrderIdAndCommandType(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.of(placeLog));
        when(commandLogAppendService.append(any(CommandLog.class)))
                .thenAnswer(invocation -> {
                    CommandLog log = invocation.getArgument(0);
                    setCommandLogId(log);
                    return log;
                });

        // when
        CommandAck ack = cancelOrder.handle(command);

        // then
        assertThat(ack.status()).isEqualTo(CommandAck.Status.ACCEPTED);
        assertThat(ack.idempotencyHit()).isFalse();
        assertThat(ack.commandType()).isEqualTo(CommandType.CANCEL_ORDER);
        assertThat(ack.commandLogId()).isEqualTo(200L);

        ArgumentCaptor<CommandLog> captor = ArgumentCaptor.forClass(CommandLog.class);
        verify(commandLogAppendService).append(captor.capture());
        CommandLog appended = captor.getValue();
        assertThat(appended.getUserId()).isEqualTo(1L);
        assertThat(appended.getMarketId()).isEqualTo(1L);
        assertThat(appended.getClientOrderId()).isEqualTo("cid-1");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("missingRequiredInputs")
    @DisplayName("Negative : 필수 입력값 누락이면 BusinessException을 반환한다.")
    void handle_with_missing_required_value(String caseName, CancelOrder.Command command) {
        // when & then
        assertThatThrownBy(() -> cancelOrder.handle(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MISSING_ORDER_REQUIRED_VALUE);
    }

    private static Stream<Arguments> missingRequiredInputs() {
        return Stream.of(
                Arguments.of("command null", null),
                Arguments.of("userId null", new CancelOrder.Command(null, 1L, "cid-1", null)),
                Arguments.of("marketId null", new CancelOrder.Command(1L, null, "cid-1", null)),
                Arguments.of("clientOrderId null", new CancelOrder.Command(1L, 1L, null, null)),
                Arguments.of("clientOrderId blank", new CancelOrder.Command(1L, 1L, "   ", null))
        );
    }

    @Test
    @DisplayName("Negative : 원 주문(PLACE_ORDER)이 없으면 BusinessException을 반환한다.")
    void handle_with_place_order_not_found() {
        // given
        CancelOrder.Command command = validCommand();
        User activeUser = User.create("u@test.com", "user", EnumStatus.ACTIVE, "pw");
        Market activeMarket = createMarket(EnumStatus.ACTIVE);

        when(idempotencyHitService.find(1L, "cid-1", CommandType.CANCEL_ORDER))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(marketRepository.findWithAssetsById(1L)).thenReturn(Optional.of(activeMarket));
        when(commandLogRepository.findByUserIdAndClientOrderIdAndCommandType(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cancelOrder.handle(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
        verify(commandLogAppendService, never()).append(any(CommandLog.class));
    }

    @Test
    @DisplayName("Negative : 취소 요청 marketId가 원 주문 marketId와 다르면 BusinessException을 반환한다.")
    void handle_with_place_order_market_mismatch() {
        // given
        CancelOrder.Command command = validCommand();
        User activeUser = User.create("u@test.com", "user", EnumStatus.ACTIVE, "pw");
        Market activeMarket = createMarket(EnumStatus.ACTIVE);
        CommandLog placeLog = CommandLog.create(new CommandLog.CreateCommand(
                "place-1",
                CommandType.PLACE_ORDER,
                999L,
                1L,
                "cid-1",
                "{\"order\":\"place\"}",
                "hash-place"
        ));

        when(idempotencyHitService.find(1L, "cid-1", CommandType.CANCEL_ORDER))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(marketRepository.findWithAssetsById(1L)).thenReturn(Optional.of(activeMarket));
        when(commandLogRepository.findByUserIdAndClientOrderIdAndCommandType(1L, "cid-1", CommandType.PLACE_ORDER))
                .thenReturn(Optional.of(placeLog));

        // when & then
        assertThatThrownBy(() -> cancelOrder.handle(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
        verify(commandLogAppendService, never()).append(any(CommandLog.class));
    }

    // 주문 취소 헬퍼
    private CancelOrder.Command validCommand() {
        return new CancelOrder.Command(1L, 1L, "cid-1", "USER_REQUEST");
    }

    // 마켓 생성 헬퍼
    private Market createMarket(EnumStatus status) {
        Asset base = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Asset quote = Asset.create("KRW", "Korean Won", (byte) 2, EnumStatus.ACTIVE);
        return Market.create(new Market.CreateCommand(
                base,
                quote,
                "KRW-BTC",
                status,
                new BigDecimal("5000"),
                new BigDecimal("1000")
        ));
    }

    // 커맨드 id생성 레플리카
    private static void setCommandLogId(CommandLog commandLog) {
        try {
            Field field = CommandLog.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(commandLog, (Long) 200L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("CommandLog id 필드 주입 실패", e);
        }
    }
}
