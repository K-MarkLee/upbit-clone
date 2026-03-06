package com.project.upbit_clone.trade.infrastructure.persistence.model;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventLog 영속 모델 테스트")
class EventLogTest {
    private CommandLog commandLog;
    private String eventId;
    private EventType eventType;
    private Long marketId;
    private Long orderId;
    private String payload;

    @BeforeEach
    void setUp() {
        commandLog = CommandLog.create(new CommandLog.CreateCommand(
                "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", "hash-1"
        ));
        eventId = "evt-1";
        eventType = EventType.ORDER_BOOK_DELTA;
        marketId = 1L;
        orderId = 100L;
        payload = "{\"event\":\"delta\"}";
    }

    @Test
    @DisplayName("Happy : 유효한 값을 넣고 생성하면 이벤트 로그가 생성된다.")
    void create_event_log_with_valid_inputs() {
        // given
        EventLog.CreateCommand command = createCommand(
                commandLog, eventId, eventType, marketId, orderId, payload
        );

        // when
        EventLog eventLog = EventLog.create(command);

        // then
        assertThat(eventLog).isNotNull();
        assertThat(eventLog.getCommandLog()).isEqualTo(commandLog);
        assertThat(eventLog.getEventId()).isEqualTo(eventId);
        assertThat(eventLog.getEventType()).isEqualTo(eventType);
        assertThat(eventLog.getMarketId()).isEqualTo(marketId);
        assertThat(eventLog.getOrderId()).isEqualTo(orderId);
        assertThat(eventLog.getPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("Happy : orderId가 null이어도 생성된다.")
    void create_event_log_with_null_order_id() {
        // given
        EventLog.CreateCommand command = createCommand(
                commandLog, eventId, eventType, marketId, null, payload
        );

        // when
        EventLog eventLog = EventLog.create(command);

        // then
        assertThat(eventLog).isNotNull();
        assertThat(eventLog.getOrderId()).isNull();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullRequiredFieldCommands")
    @DisplayName("Negative : 필수 입력값이 null이면 BusinessException을 반환한다.")
    void create_event_log_with_null_required_inputs(String caseName, EventLog.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> EventLog.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EVENT_LOG_INPUT);
    }

    private static Stream<Arguments> nullRequiredFieldCommands() {
        return Stream.of(
                Arguments.of("command null", null),
                Arguments.of("commandLog null", new EventLog.CreateCommand(
                        null, "evt-1", EventType.ORDER_BOOK_DELTA, 1L, 100L, "{\"event\":\"delta\"}"
                )),
                Arguments.of("eventId null", new EventLog.CreateCommand(
                        CommandLog.create(new CommandLog.CreateCommand(
                                "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", "hash-1"
                        )), null, EventType.ORDER_BOOK_DELTA, 1L, 100L, "{\"event\":\"delta\"}"
                )),
                Arguments.of("eventType null", new EventLog.CreateCommand(
                        CommandLog.create(new CommandLog.CreateCommand(
                                "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", "hash-1"
                        )), "evt-1", null, 1L, 100L, "{\"event\":\"delta\"}"
                )),
                Arguments.of("marketId null", new EventLog.CreateCommand(
                        CommandLog.create(new CommandLog.CreateCommand(
                                "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", "hash-1"
                        )), "evt-1", EventType.ORDER_BOOK_DELTA, null, 100L, "{\"event\":\"delta\"}"
                )),
                Arguments.of("payload null", new EventLog.CreateCommand(
                        CommandLog.create(new CommandLog.CreateCommand(
                                "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", "hash-1"
                        )), "evt-1", EventType.ORDER_BOOK_DELTA, 1L, 100L, null
                ))
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("blankRequiredFieldCommands")
    @DisplayName("Negative : 필수 문자열 입력값이 blank이면 BusinessException을 반환한다.")
    void create_event_log_with_blank_required_inputs(String caseName, EventLog.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> EventLog.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EVENT_LOG_INPUT);
    }

    private static Stream<Arguments> blankRequiredFieldCommands() {
        CommandLog commandLog = CommandLog.create(new CommandLog.CreateCommand(
                "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", "hash-1"
        ));
        return Stream.of(
                Arguments.of("eventId blank", new EventLog.CreateCommand(
                        commandLog, "", EventType.ORDER_BOOK_DELTA, 1L, 100L, "{\"event\":\"delta\"}"
                )),
                Arguments.of("eventId spaces", new EventLog.CreateCommand(
                        commandLog, "   ", EventType.ORDER_BOOK_DELTA, 1L, 100L, "{\"event\":\"delta\"}"
                )),
                Arguments.of("payload blank", new EventLog.CreateCommand(
                        commandLog, "evt-1", EventType.ORDER_BOOK_DELTA, 1L, 100L, ""
                )),
                Arguments.of("payload spaces", new EventLog.CreateCommand(
                        commandLog, "evt-1", EventType.ORDER_BOOK_DELTA, 1L, 100L, "   "
                ))
        );
    }

    // 커맨드 헬퍼
    private EventLog.CreateCommand createCommand(
            CommandLog commandLog,
            String eventId,
            EventType eventType,
            Long marketId,
            Long orderId,
            String payload
    ) {
        return new EventLog.CreateCommand(
                commandLog, eventId, eventType, marketId, orderId, payload
        );
    }
}
