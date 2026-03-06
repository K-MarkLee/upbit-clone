package com.project.upbit_clone.trade.infrastructure.persistence.model;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.LogType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConsumerOffset 영속 모델 테스트")
class ConsumerOffsetTest {
    private ConsumerOffsetId id;
    private Long lastOffset;

    @BeforeEach
    void setUp() {
        id = new ConsumerOffsetId(LogType.COMMAND, "worker-1", "market-1");
        lastOffset = 10L;
    }

    @Test
    @DisplayName("Happy : 유효한 값을 넣고 생성하면 컨슈머 오프셋이 생성된다.")
    void create_consumer_offset_with_valid_inputs() {
        // when
        ConsumerOffset offset = ConsumerOffset.create(id, lastOffset);

        // then
        assertThat(offset).isNotNull();
        assertThat(offset.getId()).isEqualTo(id);
        assertThat(offset.getLastOffset()).isEqualTo(lastOffset);
    }

    @Test
    @DisplayName("Happy : lastOffset이 0이어도 생성된다.")
    void create_consumer_offset_with_zero_offset() {
        // when
        ConsumerOffset offset = ConsumerOffset.create(id, 0L);

        // then
        assertThat(offset).isNotNull();
        assertThat(offset.getLastOffset()).isZero();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullCreateInputs")
    @DisplayName("Negative : 필수 입력값이 null이면 BusinessException을 반환한다.")
    void create_consumer_offset_with_null_inputs(String caseName, ConsumerOffsetId id, Long lastOffset) {
        // when & then
        assertThatThrownBy(() -> ConsumerOffset.create(id, lastOffset))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CONSUMER_OFFSET_INPUT);
    }

    private static Stream<Arguments> nullCreateInputs() {
        ConsumerOffsetId id = new ConsumerOffsetId(LogType.COMMAND, "worker-1", "market-1");
        return Stream.of(
                Arguments.of("id null", null, 10L),
                Arguments.of("lastOffset null", id, null)
        );
    }

    @Test
    @DisplayName("Negative : lastOffset이 음수면 BusinessException을 반환한다.")
    void create_consumer_offset_with_negative_offset() {
        // when & then
        assertThatThrownBy(() -> ConsumerOffset.create(id, -1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NEGATIVE_OFFSET_NOT_ALLOWED);
    }
}
