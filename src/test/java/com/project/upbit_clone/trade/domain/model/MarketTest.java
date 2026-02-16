package com.project.upbit_clone.trade.domain.model;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
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

@DisplayName("Market 도메인 테스트")
class MarketTest {
    private Asset baseAsset;
    private Asset quoteAsset;
    private String marketCode;
    private EnumStatus status;
    private BigDecimal minOrderQuote;
    private BigDecimal tickSize;

    @BeforeEach
    void setUp() {
        baseAsset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        quoteAsset = Asset.create("KRW", "Korean Won", (byte) 2, EnumStatus.ACTIVE);
        marketCode = "KRW-BTC";
        status = EnumStatus.ACTIVE;
        minOrderQuote = BigDecimal.ZERO;
        tickSize = new BigDecimal("1000");
    }

    @Test
    @DisplayName("Happy : 유효한 값을 넣고 생성하면 마켓이 생성된다.")
    void create_market_with_valid_inputs() {
        // given
        Market.CreateCommand command = createCommand(
                baseAsset, quoteAsset, marketCode, status, minOrderQuote, tickSize
        );

        // when
        Market market = Market.create(command);

        // then
        assertThat(market).isNotNull();
        assertThat(market.getBaseAsset()).isEqualTo(baseAsset);
        assertThat(market.getQuoteAsset()).isEqualTo(quoteAsset);
        assertThat(market.getMarketCode()).isEqualTo(marketCode);
        assertThat(market.getStatus()).isEqualTo(status);
        assertThat(market.getMinOrderQuote()).isEqualTo(minOrderQuote);
        assertThat(market.getTickSize()).isEqualTo(tickSize);
    }

    @Test
    @DisplayName("Happy : 상태값에 null을 넣고 생성하면 디폴트값으로 마켓이 생성된다.")
    void create_market_with_null_status() {
        // given
        Market.CreateCommand command = createCommand(
                baseAsset, quoteAsset, marketCode, null, minOrderQuote, tickSize
        );

        // when
        Market market = Market.create(command);

        // then
        assertThat(market).isNotNull();
        assertThat(market.getStatus()).isEqualTo(EnumStatus.ACTIVE);

    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullRequiredFieldCommands")
    @DisplayName("Negative : 필수 입력값이 null이면 BusinessException을 반환한다.")
    void create_market_with_null_inputs(String caseName, Market.CreateCommand command) {
        assertThatThrownBy(() -> Market.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MARKET_INPUT);
    }

    private static Stream<Arguments> nullRequiredFieldCommands() {
        Asset baseAsset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Asset quoteAsset = Asset.create("KRW", "Korean Won", (byte) 2, EnumStatus.ACTIVE);
        String marketCode = "KRW-BTC";
        BigDecimal minOrderQuote = BigDecimal.ZERO;
        BigDecimal tickSize = new BigDecimal("1000");

        return Stream.of(
                Arguments.of("command null", null),
                Arguments.of("baseAsset null", new Market.CreateCommand(null, quoteAsset, marketCode, EnumStatus.ACTIVE, minOrderQuote, tickSize)),
                Arguments.of("quoteAsset null", new Market.CreateCommand(baseAsset, null, marketCode, EnumStatus.ACTIVE, minOrderQuote, tickSize)),
                Arguments.of("marketCode null", new Market.CreateCommand(baseAsset, quoteAsset, null, EnumStatus.ACTIVE, minOrderQuote, tickSize)),
                Arguments.of("minOrderQuote null", new Market.CreateCommand(baseAsset, quoteAsset, marketCode, EnumStatus.ACTIVE, null, tickSize)),
                Arguments.of("tickSize null", new Market.CreateCommand(baseAsset, quoteAsset, marketCode, EnumStatus.ACTIVE, minOrderQuote, null))
        );
    }

    @Test
    @DisplayName("Negative : 필수 입력값이 blank이면 BusinessException을 반환한다.")
    void create_market_with_blank_inputs() {
        // given
        Market.CreateCommand command = createCommand(
                baseAsset, quoteAsset, "", null, minOrderQuote, tickSize
        );

        // when&then
        // marketCode blank
        assertThatThrownBy(() -> Market.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MARKET_INPUT);

    }

    @Test
    @DisplayName("Negative : 필수 입력값이 blank이면 BusinessException을 반환한다.")
    void create_market_with_blank_inputs_2() {
        // given
        Market.CreateCommand command = createCommand(
                baseAsset, quoteAsset, "   ", null, minOrderQuote, tickSize
        );

        // when&then
        assertThatThrownBy(() -> Market.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_MARKET_INPUT);

    }

    // 커맨드 생성 헬퍼
    private Market.CreateCommand createCommand(
            Asset baseAsset,
            Asset quoteAsset,
            String marketCode,
            EnumStatus status,
            BigDecimal minOrderQuote,
            BigDecimal tickSize
    ) {
        return new Market.CreateCommand(
                baseAsset, quoteAsset, marketCode, status, minOrderQuote, tickSize
        );
    }
}
