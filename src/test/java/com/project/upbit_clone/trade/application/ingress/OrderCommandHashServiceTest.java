package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderCommandHashService 단위 테스트")
class OrderCommandHashServiceTest {

    private final OrderCommandHashService hashService = new OrderCommandHashService();

    @Test
    @DisplayName("Happy : 정규화된 PLACE_ORDER는 멱등 키가 달라도 같은 해시를 생성한다.")
    void hash_place_order_ignores_idempotency_key_fields() {
        // given
        PlaceOrder.Command left = new PlaceOrder.Command(
                1L, 1L, "cid-1",
                OrderSide.BID, OrderType.LIMIT, TimeInForce.GTC,
                new BigDecimal("10000"), new BigDecimal("1.23"), null
        );
        PlaceOrder.Command right = new PlaceOrder.Command(
                2L, 1L, "cid-2",
                OrderSide.BID, OrderType.LIMIT, TimeInForce.GTC,
                new BigDecimal("10000"), new BigDecimal("1.23"), null
        );

        // when
        String leftHash = hashService.hash(left);
        String rightHash = hashService.hash(right);

        // then
        assertThat(leftHash).isEqualTo(rightHash);
    }

    @Test
    @DisplayName("Negative : command가 null이면 BusinessException을 반환한다.")
    void hash_with_null_command() {
        // when & then
        assertThatThrownBy(() -> hashService.hash(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MISSING_ORDER_REQUIRED_VALUE);
    }

    @Test
    @DisplayName("Happy : 정규화된 decimal은 plain string 기준으로 같은 해시를 생성한다.")
    void hash_place_order_uses_plain_string_for_decimal() {
        // given
        BigDecimal scientific = new BigDecimal("10000.0").stripTrailingZeros();
        PlaceOrder.Command right = new PlaceOrder.Command(
                1L, 1L, "cid-1",
                OrderSide.BID, OrderType.MARKET, TimeInForce.IOC,
                null, null, new BigDecimal("10000")
        );
        PlaceOrder.Command left = new PlaceOrder.Command(
                2L, 1L, "cid-2",
                OrderSide.BID, OrderType.MARKET, TimeInForce.IOC,
                null, null, scientific
        );

        // when
        String leftHash = hashService.hash(left);
        String rightHash = hashService.hash(right);

        // then
        assertThat(leftHash).isEqualTo(rightHash);
    }

    @Test
    @DisplayName("Happy : 정규화된 CANCEL_ORDER는 멱등 키가 달라도 같은 해시를 생성한다.")
    void hash_cancel_order_ignores_idempotency_key_fields() {
        // given
        CancelOrder.Command left = new CancelOrder.Command(1L, 1L, "cid-1");
        CancelOrder.Command right = new CancelOrder.Command(2L, 1L, "cid-2");

        // when
        String leftHash = hashService.hash(left);
        String rightHash = hashService.hash(right);

        // then
        assertThat(leftHash).isEqualTo(rightHash);
    }

    @Test
    @DisplayName("Negative : CANCEL_ORDER에서 marketId가 다르면 해시가 달라진다.")
    void hash_cancel_order_with_different_market_id() {
        // given
        CancelOrder.Command left = new CancelOrder.Command(1L, 1L, "cid-1");
        CancelOrder.Command right = new CancelOrder.Command(1L, 2L, "cid-1");

        // when
        String leftHash = hashService.hash(left);
        String rightHash = hashService.hash(right);

        // then
        assertThat(leftHash).isNotEqualTo(rightHash);
    }

    @Test
    @DisplayName("Negative : 비즈니스 값이 다르면 해시가 달라진다.")
    void hash_with_different_business_value() {
        // given
        PlaceOrder.Command left = new PlaceOrder.Command(
                1L, 1L, "cid-1",
                OrderSide.BID, OrderType.LIMIT, TimeInForce.GTC,
                new BigDecimal("10000"), BigDecimal.ONE, null
        );
        PlaceOrder.Command right = new PlaceOrder.Command(
                1L, 1L, "cid-1",
                OrderSide.BID, OrderType.LIMIT, TimeInForce.GTC,
                new BigDecimal("11000"), BigDecimal.ONE, null
        );

        // when
        String leftHash = hashService.hash(left);
        String rightHash = hashService.hash(right);

        // then
        assertThat(leftHash).isNotEqualTo(rightHash);
    }

    @Test
    @DisplayName("Negative : 지원하지 않는 command 타입이면 BusinessException을 반환한다.")
    void hash_with_unsupported_command_type() {
        // given
        OrderCommand unknown = new UnknownCommand(1L, 1L, "cid-1");

        // when & then
        assertThatThrownBy(() -> hashService.hash(unknown))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_COMMAND_TYPE);
    }


    private record UnknownCommand(Long userId, Long marketId, String clientOrderId) implements OrderCommand {
        @Override
        public CommandType commandType() {
            return CommandType.PLACE_ORDER;
        }
    }
}
