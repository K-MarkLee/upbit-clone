package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Command Ack 단위 테스트")
class CommandAckTest {

    @ParameterizedTest(name = "[{index}] idempotencyHit={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("Happy : accepted는 CommandLog 값을 매핑해 ACK를 생성한다.")
    void accepted_maps_from_command_log(boolean idempotencyHit) {
        // given
        CommandLog commandLog = CommandLog.create(new CommandLog.CreateCommand(
                "cmd-1",
                CommandType.PLACE_ORDER,
                1L,
                1L,
                "cid-1",
                "{\"payload\":1}",
                "hash-1"
        ));
        setCommandLogId(commandLog, 101L);

        // when
        CommandAck ack = CommandAck.accepted(commandLog, idempotencyHit);

        // then
        assertThat(ack.commandId()).isEqualTo("cmd-1");
        assertThat(ack.commandLogId()).isEqualTo(101L);
        assertThat(ack.commandType()).isEqualTo(CommandType.PLACE_ORDER);
        assertThat(ack.status()).isEqualTo(CommandAck.Status.ACCEPTED);
        assertThat(ack.idempotencyHit()).isEqualTo(idempotencyHit);
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
