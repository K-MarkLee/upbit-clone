package com.project.upbit_clone.user.domain.model;

import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 도메인 테스트")
class UserTest {
    private String email;
    private String userName;
    private EnumStatus status;
    private String passwordHash;

    @BeforeEach
    void setUp() {
        email = "example@example.com";
        userName = "example";
        status = EnumStatus.ACTIVE;
        passwordHash = "hashed-password";
    }

    @Test
    @DisplayName("Happy : 유효한 값을 넣고 생성하면 사용자가 생성된다.")
    void create_user_with_valid_inputs() {
        // when
        User user = User.create(email, userName, status, passwordHash);

        // then
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getUserName()).isEqualTo(userName);
        assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
    }

    @Test
    @DisplayName("Happy : 유효한 상태값을 넣고 생성하면 상태가 유지된다.")
    void create_user_with_status() {
        // when
        User user = User.create(email, userName, EnumStatus.INACTIVE, passwordHash);

        // then
        assertThat(user.getStatus()).isEqualTo(EnumStatus.INACTIVE);
    }

    @Test
    @DisplayName("Happy : 상태값을 null 로 생성하면 ACTIVE로 생성된다.")
    void create_user_with_null_status() {
        // given & when
        User user = User.create(email, userName, null, passwordHash);

        // then
        assertThat(user.getStatus()).isEqualTo(EnumStatus.ACTIVE);
    }

    @Test
    @DisplayName("Negative : 입력값이 null이면 BusinessException을 반환한다.")
    void create_user_with_null_inputs() {
        // when & then
        // email null
        assertThatThrownBy(()-> User.create(null, userName, status, passwordHash))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USER_INPUT);

        // userName null
        assertThatThrownBy(()-> User.create(email, null, status, passwordHash))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USER_INPUT);

        // passwordHash null
        assertThatThrownBy(()-> User.create(email, userName, status, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USER_INPUT);
    }

    @Test
    @DisplayName("Negative : 입력값이 blank면 BusinessException을 반환한다.")
    void create_user_with_blank_inputs() {
        // when & then
        // email blank
        assertThatThrownBy(()-> User.create("", userName, status, passwordHash))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USER_INPUT);

        // userName blank
        assertThatThrownBy(()-> User.create(email, "", status, passwordHash))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USER_INPUT);

        // passwordHash blank
        assertThatThrownBy(()-> User.create(email, userName, status, ""))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USER_INPUT);
    }

    @Test
    @DisplayName("Negative : 입력값이 blank면 BusinessException을 반환한다.")
    void create_user_with_blank_inputs_2() {
        // when & then
        // email blank
        assertThatThrownBy(()-> User.create("   ", userName, status, passwordHash))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USER_INPUT);

        // userName blank
        assertThatThrownBy(()-> User.create(email, "   ", status, passwordHash))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USER_INPUT);

        // passwordHash blank
        assertThatThrownBy(()-> User.create(email, userName, status, "   "))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_USER_INPUT);
    }

}
