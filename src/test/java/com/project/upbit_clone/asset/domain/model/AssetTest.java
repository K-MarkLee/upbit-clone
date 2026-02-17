package com.project.upbit_clone.asset.domain.model;

import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Asset 도메인 테스트")
class AssetTest {
    private String symbol;
    private String name;
    private Byte decimals;
    private EnumStatus status;

    @BeforeEach
    void setUp() {
        symbol = "BTC";
        name = "Bitcoin";
        decimals = 8;
        status = EnumStatus.ACTIVE;
    }

    @Test
    @DisplayName("Happy : 유효한 값을 넣고 생성하면 자산이 생성된다.")
    void create_asset_with_valid_inputs() {
        // when
        Asset asset = Asset.create(symbol, name, decimals, status);

        // then
        assertThat(asset).isNotNull();
        assertThat(asset.getSymbol()).isEqualTo(symbol);
        assertThat(asset.getName()).isEqualTo(name);
        assertThat(asset.getDecimals()).isEqualTo(decimals);
        assertThat(asset.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Happy : 상태값에 null을 넣고 생성하면 Default값으로 자산이 생성된다.")
    void create_asset_with_null_status() {
        // when
        Asset asset = Asset.create(symbol, name, decimals, null);

        // then
        assertThat(asset.getStatus()).isEqualTo(EnumStatus.ACTIVE);
    }

    @Test
    @DisplayName("Negative : 입력값이 null이면 BusinessException을 반환한다.")
    void create_asset_with_null_inputs() {
        // when & then
        // symbol null
        assertThatThrownBy(() -> Asset.create(null, name, decimals, status))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ASSET_INPUT);

        // name null
        assertThatThrownBy(() -> Asset.create(symbol, null, decimals, status))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ASSET_INPUT);

        // decimals null
        assertThatThrownBy(() -> Asset.create(symbol, name, null, status))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ASSET_INPUT);
    }

    @Test
    @DisplayName("Negative : 입력값이 blank이면 BusinessException을 반환한다.")
    void create_asset_with_blank_inputs() {
        // when & then
        // symbol blank
        assertThatThrownBy(() -> Asset.create("", name, decimals, status))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ASSET_INPUT);

        // name blank
        assertThatThrownBy(() -> Asset.create(symbol, "", decimals, status))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ASSET_INPUT);
    }

    @Test
    @DisplayName("Negative : 입력값이 blank이면 BusinessException을 반환한다.")
    void create_asset_with_blank_inputs_2() {
        // when & then
        // symbol blank
        assertThatThrownBy(() -> Asset.create("   ", name, decimals, status))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ASSET_INPUT);

        // name blank
        assertThatThrownBy(() -> Asset.create(symbol, "   ", decimals, status))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ASSET_INPUT);
    }
}
