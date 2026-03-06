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

    @Test
    @DisplayName("Happy : lock 호출 시 available에서 locked로 금액이 이동한다.")
    void lock_with_sufficient_available_balance() {
        // given
        Wallet wallet = Wallet.create(user, asset, new BigDecimal("100"), BigDecimal.ZERO);

        // when
        wallet.lock(new BigDecimal("30"));

        // then
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("70");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("Negative : lock 호출 시 available이 부족하면 BusinessException을 반환한다.")
    void lock_with_insufficient_available_balance() {
        // given
        Wallet wallet = Wallet.create(user, asset, new BigDecimal("10"), BigDecimal.ZERO);
        BigDecimal amount = new BigDecimal("30");

        // when & then
        assertThatThrownBy(() -> wallet.lock(amount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_AVAILABLE_BALANCE);
    }

    @Test
    @DisplayName("Happy : unlock 호출 시 locked에서 available로 금액이 이동한다.")
    void unlock_with_sufficient_locked_balance() {
        // given
        Wallet wallet = Wallet.create(user, asset, new BigDecimal("70"), new BigDecimal("30"));

        // when
        wallet.unlock(new BigDecimal("20"));

        // then
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("90");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Negative : unlock 호출 시 locked가 부족하면 BusinessException을 반환한다.")
    void unlock_with_insufficient_locked_balance() {
        // given
        Wallet wallet = Wallet.create(user, asset, new BigDecimal("70"), new BigDecimal("10"));
        BigDecimal amount = new BigDecimal("20");

        // when & then
        assertThatThrownBy(() -> wallet.unlock(amount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_LOCKED_BALANCE);
    }

    @Test
    @DisplayName("Happy : decreaseLocked 호출 시 locked만 감소한다.")
    void decrease_locked_with_sufficient_locked_balance() {
        // given
        Wallet wallet = Wallet.create(user, asset, new BigDecimal("70"), new BigDecimal("30"));

        // when
        wallet.decreaseLocked(new BigDecimal("20"));

        // then
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("70");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Negative : decreaseLocked 호출 시 locked가 부족하면 BusinessException을 반환한다.")
    void decrease_locked_with_insufficient_locked_balance() {
        // given
        Wallet wallet = Wallet.create(user, asset, new BigDecimal("70"), new BigDecimal("10"));
        BigDecimal amount = new BigDecimal("20");
        // when & then
        assertThatThrownBy(() -> wallet.decreaseLocked(amount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_LOCKED_BALANCE);
    }

    @Test
    @DisplayName("Happy : increaseAvailable 호출 시 available만 증가한다.")
    void increase_available() {
        // given
        Wallet wallet = Wallet.create(user, asset, new BigDecimal("70"), new BigDecimal("10"));

        // when
        wallet.increaseAvailable(new BigDecimal("20"));

        // then
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("90");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Negative : increaseAvailable 호출 시 금액이 0 이하면 IllegalArgumentException을 반환한다.")
    void increase_available_with_non_positive_amount() {
        // given
        Wallet wallet = Wallet.create(user, asset, new BigDecimal("70"), new BigDecimal("10"));

        // when & then
        assertThatThrownBy(() -> wallet.increaseAvailable(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
