package com.project.upbit_clone.wallet.domain.model;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Wallet 도메인 테스트")
class WalletTest {
    private User user;
    private Asset asset;
    private BigDecimal availableBalance;
    private BigDecimal lockedBalance;

    @BeforeEach
    void setUp() {
        user = User.create("test@test.com", "test", EnumStatus.ACTIVE, "hashed-password");
        asset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        availableBalance = new BigDecimal("10");
        lockedBalance = new BigDecimal("0.0003");
    }

    @Test
    @DisplayName("Happy : 유효한 값을 넣고 생성하면 지갑이 생성된다.")
    void create_wallet_with_valid_inputs() {
        // when
        Wallet wallet = Wallet.create(user, asset, availableBalance, lockedBalance);

        // then
        assertThat(wallet).isNotNull();
        assertThat(wallet.getAsset()).isEqualTo(asset);
        assertThat(wallet.getAvailableBalance()).isEqualTo(availableBalance);
        assertThat(wallet.getLockedBalance()).isEqualTo(lockedBalance);
    }

    @Test
    @DisplayName("Happy : available_balance에 null값을 넣으면 디폴트 값으로 지갑이 생성된다.")
    void create_wallet_with_null_available_balance() {
        // when
        Wallet wallet = Wallet.create(user, asset, null, lockedBalance);

        // then
        assertThat(wallet).isNotNull();
        assertThat(wallet.getAsset()).isEqualTo(asset);
        assertThat(wallet.getAvailableBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(wallet.getLockedBalance()).isEqualTo(lockedBalance);
    }

    @Test
    @DisplayName("Happy : locked_balance에 null값을 넣으면 디폴트 값으로 지갑이 생성된다.")
    void create_wallet_with_null_locked_balance() {
        // when
        Wallet wallet = Wallet.create(user, asset, availableBalance, null);

        // then
        assertThat(wallet).isNotNull();
        assertThat(wallet.getAsset()).isEqualTo(asset);
        assertThat(wallet.getAvailableBalance()).isEqualTo(availableBalance);
        assertThat(wallet.getLockedBalance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Negative : 필수 입력값이 null이면 BusinessException을 반환한다.")
    void create_wallet_with_null_inputs() {
        // when & then
        // user null
        assertThatThrownBy(() -> Wallet.create(null, asset, availableBalance, lockedBalance))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_WALLET_INPUT);

        // asset null
        assertThatThrownBy(() -> Wallet.create(user, null, availableBalance, lockedBalance))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_WALLET_INPUT);
    }

}
