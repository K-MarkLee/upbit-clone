package com.project.upbit_clone.user.domain.model;

import com.project.upbit_clone.global.domain.vo.EnumStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User 도메인 테스트")
class UserTest {

    @Test
    @DisplayName("유효한 값을 넣고 생성하면 사용자가 생성된다.")
    void create_user_with_valid_inputs() {
        // given
        String email = "alpha@example.com";
        String userName = "alpha";
        EnumStatus status = EnumStatus.ACTIVE;
        String passwordHash = "hashed-password";

        // when
        User user = User.create(email, userName, status, passwordHash);

        // then
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getUserName()).isEqualTo(userName);
        assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
    }

    @Test
    @DisplayName("유효한 상태값을 넣고 생성하면 상태가 유지된다.")
    void create_user_with_status() {
        // given
        EnumStatus status = EnumStatus.INACTIVE;

        // when
        User user = User.create("beta@example.com", "beta", status, "pw");

        // then
        assertThat(user.getStatus()).isEqualTo(status);
    }
}
