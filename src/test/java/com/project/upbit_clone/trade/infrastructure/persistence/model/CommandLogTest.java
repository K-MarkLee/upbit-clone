package com.project.upbit_clone.trade.infrastructure.persistence.model;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CommandLog 영속 모델 테스트")
class CommandLogTest {
    private String commandId;
    private CommandType commandType;
    private Long marketId;
    private Long userId;
    private String clientOrderId;
    private String payload;
    private String requestHash;

    @BeforeEach
    void setUp() {
        commandId = "cmd-1";
        commandType = CommandType.PLACE_ORDER;
        marketId = 1L;
        userId = 10L;
        clientOrderId = "client-1";
        payload = "{\"a\":1}";
        requestHash = "hash-1";
    }

    @Test
    @DisplayName("Happy : 유효한 값을 넣고 생성하면 커맨드 로그가 생성된다.")
    void create_command_log_with_valid_inputs() {
        // given
        CommandLog.CreateCommand command = createCommand(
                commandId, commandType, marketId, userId, clientOrderId, payload, requestHash
        );

        // when
        CommandLog commandLog = CommandLog.create(command);

        // then
        assertThat(commandLog).isNotNull();
        assertThat(commandLog.getCommandId()).isEqualTo(commandId);
        assertThat(commandLog.getCommandType()).isEqualTo(commandType);
        assertThat(commandLog.getMarketId()).isEqualTo(marketId);
        assertThat(commandLog.getUserId()).isEqualTo(userId);
        assertThat(commandLog.getClientOrderId()).isEqualTo(clientOrderId);
        assertThat(commandLog.getPayload()).isEqualTo(payload);
        assertThat(commandLog.getRequestHash()).isEqualTo(requestHash);
    }

    @Test
    @DisplayName("Happy : userId와 clientOrderId가 null이어도 생성된다.")
    void create_command_log_with_nullable_fields() {
        // given
        CommandLog.CreateCommand command = createCommand(
                commandId, commandType, marketId, null, null, payload, requestHash
        );

        // when
        CommandLog commandLog = CommandLog.create(command);

        // then
        assertThat(commandLog).isNotNull();
        assertThat(commandLog.getUserId()).isNull();
        assertThat(commandLog.getClientOrderId()).isNull();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullRequiredFieldCommands")
    @DisplayName("Negative : 필수 입력값이 null이면 BusinessException을 반환한다.")
    void create_command_log_with_null_required_inputs(String caseName, CommandLog.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> CommandLog.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_COMMAND_LOG_INPUT);
    }

    private static Stream<Arguments> nullRequiredFieldCommands() {
        return Stream.of(
                Arguments.of("command null", null),
                Arguments.of("commandId null", new CommandLog.CreateCommand(
                        null, CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", "hash-1"
                )),
                Arguments.of("commandType null", new CommandLog.CreateCommand(
                        "cmd-1", null, 1L, 10L, "client-1", "{\"a\":1}", "hash-1"
                )),
                Arguments.of("marketId null", new CommandLog.CreateCommand(
                        "cmd-1", CommandType.PLACE_ORDER, null, 10L, "client-1", "{\"a\":1}", "hash-1"
                )),
                Arguments.of("payload null", new CommandLog.CreateCommand(
                        "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", null, "hash-1"
                )),
                Arguments.of("requestHash null", new CommandLog.CreateCommand(
                        "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", null
                ))
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("blankRequiredFieldCommands")
    @DisplayName("Negative : 필수 입력값이 blank이면 BusinessException을 반환한다.")
    void create_command_log_with_blank_required_inputs(String caseName, CommandLog.CreateCommand command) {
        // when & then
        assertThatThrownBy(() -> CommandLog.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_COMMAND_LOG_INPUT);
    }

    private static Stream<Arguments> blankRequiredFieldCommands() {
        return Stream.of(
                Arguments.of("commandId blank", new CommandLog.CreateCommand(
                        "", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", "hash-1"
                )),
                Arguments.of("commandId spaces", new CommandLog.CreateCommand(
                        "   ", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", "hash-1"
                )),
                Arguments.of("payload blank", new CommandLog.CreateCommand(
                        "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "", "hash-1"
                )),
                Arguments.of("payload spaces", new CommandLog.CreateCommand(
                        "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "   ", "hash-1"
                )),
                Arguments.of("requestHash blank", new CommandLog.CreateCommand(
                        "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", ""
                )),
                Arguments.of("requestHash spaces", new CommandLog.CreateCommand(
                        "cmd-1", CommandType.PLACE_ORDER, 1L, 10L, "client-1", "{\"a\":1}", "   "
                ))
        );
    }

    // 커맨드 헬퍼
    private CommandLog.CreateCommand createCommand(
            String commandId,
            CommandType commandType,
            Long marketId,
            Long userId,
            String clientOrderId,
            String payload,
            String requestHash
    ) {
        return new CommandLog.CreateCommand(
                commandId, commandType, marketId, userId, clientOrderId, payload, requestHash
        );
    }
}
