package com.project.upbit_clone.trade.infrastructure.persistence.model;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
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

@DisplayName("OrderBookProjection 영속 모델 테스트")
class OrderBookProjectionTest {
    private OrderBookProjectionId id;
    private BigDecimal totalQty;
    private Integer orderCount;

    @BeforeEach
    void setUp() {
        id = new OrderBookProjectionId(1L, OrderSide.BID, new BigDecimal("1000"));
        totalQty = new BigDecimal("2.5");
        orderCount = 3;
    }

    @Test
    @DisplayName("Happy : 유효한 값을 넣고 생성하면 오더북 프로젝션이 생성된다.")
    void create_order_book_projection_with_valid_inputs() {
        // when
        OrderBookProjection projection = OrderBookProjection.create(id, totalQty, orderCount);

        // then
        assertThat(projection).isNotNull();
        assertThat(projection.getId()).isEqualTo(id);
        assertThat(projection.getTotalQty()).isEqualByComparingTo(totalQty);
        assertThat(projection.getOrderCount()).isEqualTo(orderCount);
    }

    @Test
    @DisplayName("Happy : totalQty와 orderCount가 0이어도 생성된다.")
    void create_order_book_projection_with_zero_values() {
        // when
        OrderBookProjection projection = OrderBookProjection.create(id, BigDecimal.ZERO, 0);

        // then
        assertThat(projection).isNotNull();
        assertThat(projection.getTotalQty()).isZero();
        assertThat(projection.getOrderCount()).isZero();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullCreateInputs")
    @DisplayName("Negative : 필수 입력값이 null이면 BusinessException을 반환한다.")
    void create_order_book_projection_with_null_inputs(String caseName, OrderBookProjectionId id, BigDecimal totalQty, Integer orderCount) {
        // when & then
        assertThatThrownBy(() -> OrderBookProjection.create(id, totalQty, orderCount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_BOOK_PROJECTION_INPUT);
    }

    private static Stream<Arguments> nullCreateInputs() {
        OrderBookProjectionId id = new OrderBookProjectionId(1L, OrderSide.BID, new BigDecimal("1000"));
        BigDecimal qty = new BigDecimal("2.5");
        return Stream.of(
                Arguments.of("id null", null, qty, 3),
                Arguments.of("totalQty null", id, null, 3),
                Arguments.of("orderCount null", id, qty, null)
        );
    }

    @Test
    @DisplayName("Negative : orderCount가 음수면 BusinessException을 반환한다.")
    void create_order_book_projection_with_negative_order_count() {
        // when & then
        assertThatThrownBy(() -> OrderBookProjection.create(id, totalQty, -1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NEGATIVE_ORDER_COUNT_NOT_ALLOWED);
    }

    @Test
    @DisplayName("Negative : totalQty가 음수면 IllegalArgumentException을 반환한다.")
    void create_order_book_projection_with_negative_total_qty() {
        BigDecimal qty = new BigDecimal("-0.1");
        Integer count = 1;
        // when & then
        assertThatThrownBy(() -> OrderBookProjection.create(id, qty, count))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
