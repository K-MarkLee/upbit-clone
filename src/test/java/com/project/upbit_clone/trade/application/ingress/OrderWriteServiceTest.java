package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderWriteService 단위 테스트")
class OrderWriteServiceTest {

    @Mock
    private CommandLogRepository commandLogRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WalletRepository walletRepository;

    private OrderWriteService orderWriteService;

    @BeforeEach
    void setUp() {
        orderWriteService = new OrderWriteService(
                commandLogRepository,
                orderRepository,
                walletRepository
        );
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
        Asset baseAsset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Asset quoteAsset = Asset.create("KRW", "Korean Won", (byte) 8, EnumStatus.ACTIVE);
        return Market.create(new Market.CreateCommand(
                baseAsset,
                quoteAsset,
                EnumStatus.ACTIVE,
                new BigDecimal("5000"),
                new BigDecimal("1000")
        ));
    }
}
