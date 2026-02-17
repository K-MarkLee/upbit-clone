package com.project.upbit_clone.wallet.domain.model;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.wallet.domain.vo.ChangeType;
import com.project.upbit_clone.wallet.domain.vo.LedgerType;
import com.project.upbit_clone.wallet.domain.vo.ReferenceType;
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

@DisplayName("Ledger 도메인 테스트")
class LedgerTest {
    private Wallet wallet;
    private Asset asset;
    private LedgerType ledgerType ;
    private ChangeType changeType;
    private BigDecimal amount;
    private BigDecimal availableBefore;
    private BigDecimal availableAfter;
    private BigDecimal lockedBefore;
    private BigDecimal lockedAfter;
    private ReferenceType referenceType;
    private Long referenceId;
    private String description;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        User user = User.create("test@test.com", "test", EnumStatus.ACTIVE, "hashed-password");
        asset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        wallet = Wallet.create(user, asset, BigDecimal.ZERO, BigDecimal.ZERO);
        ledgerType = LedgerType.ORDER_LOCK;
        changeType = ChangeType.DECREASE;
        amount = new BigDecimal("10");
        availableBefore = new BigDecimal("100");
        availableAfter = new BigDecimal("90");
        lockedBefore = new BigDecimal("10");
        lockedAfter = new BigDecimal("20");
        referenceType = ReferenceType.ORDER;
        referenceId = null;
        description = null;
        idempotencyKey = "1";
    }

    @Test
    @DisplayName("Happy : 유효한 값을 넣고 생성하면 원장이 생성된다. ")
    void create_ledger_with_valid_inputs() {
        // given
        Ledger.CreateCommand command = createCommand(
                wallet, asset, ledgerType, changeType, amount, availableBefore, availableAfter, lockedBefore, lockedAfter, referenceType, referenceId, description, idempotencyKey
        );


        // when
        Ledger ledger = Ledger.create(command);

        // then
        assertThat(ledger).isNotNull();
        assertThat(ledger.getWallet()).isEqualTo(wallet);
        assertThat(ledger.getAsset()).isEqualTo(asset);
        assertThat(ledger.getLedgerType()).isEqualTo(ledgerType);
        assertThat(ledger.getChangeType()).isEqualTo(changeType);
        assertThat(ledger.getAmount()).isEqualTo(amount);
        assertThat(ledger.getAvailableBefore()).isEqualTo(availableBefore);
        assertThat(ledger.getAvailableAfter()).isEqualTo(availableAfter);
        assertThat(ledger.getLockedBefore()).isEqualTo(lockedBefore);
        assertThat(ledger.getLockedAfter()).isEqualTo(lockedAfter);
        assertThat(ledger.getReferenceType()).isEqualTo(referenceType);
        assertThat(ledger.getReferenceId()).isEqualTo(referenceId);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullRequiredFieldCommands")
    @DisplayName("Negative : 필수 입력값이 null이면 BusinessException을 반환한다.")
    void create_ledger_with_null_inputs(String caseName, Ledger.CreateCommand command) {
        assertThatThrownBy(() -> Ledger.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LEDGER_INPUT);
    }

    private static Stream<Arguments> nullRequiredFieldCommands() {
        User user = User.create("test@test.com", "test", EnumStatus.ACTIVE, "hashed-password");
        Asset asset = Asset.create("BTC", "Bitcoin", (byte) 8, EnumStatus.ACTIVE);
        Wallet wallet = Wallet.create(user, asset, BigDecimal.ZERO, BigDecimal.ZERO);
        LedgerType ledgerType = LedgerType.ORDER_LOCK;
        ChangeType changeType = ChangeType.DECREASE;
        BigDecimal amount = new BigDecimal("10");
        BigDecimal availableBefore = new BigDecimal("100");
        BigDecimal availableAfter = new BigDecimal("90");
        BigDecimal lockedBefore = new BigDecimal("10");
        BigDecimal lockedAfter = new BigDecimal("20");
        ReferenceType referenceType = ReferenceType.ORDER;
        String idempotencyKey = "1";

        return Stream.of(
                Arguments.of("command nul", null),
                Arguments.of("wallet null", new Ledger.CreateCommand(null, asset, ledgerType,changeType, amount, availableBefore, availableAfter, lockedBefore, lockedAfter, referenceType, null, null, idempotencyKey)),
                Arguments.of("asset null", new Ledger.CreateCommand(wallet, null, ledgerType,changeType, amount, availableBefore, availableAfter, lockedBefore, lockedAfter, referenceType, null, null, idempotencyKey)),
                Arguments.of("ledger_type null", new Ledger.CreateCommand(wallet, asset, null,changeType, amount, availableBefore, availableAfter, lockedBefore, lockedAfter, referenceType, null, null, idempotencyKey)),
                Arguments.of("change_type null", new Ledger.CreateCommand(wallet, asset, ledgerType,null, amount, availableBefore, availableAfter, lockedBefore, lockedAfter, referenceType, null, null, idempotencyKey)),
                Arguments.of("amount null", new Ledger.CreateCommand(wallet, asset, ledgerType,changeType, null, availableBefore, availableAfter, lockedBefore, lockedAfter, referenceType, null, null, idempotencyKey)),
                Arguments.of("available_before null", new Ledger.CreateCommand(wallet, asset, ledgerType,changeType, amount, null, availableAfter, lockedBefore, lockedAfter, referenceType, null, null, idempotencyKey)),
                Arguments.of("available_after null", new Ledger.CreateCommand(wallet, asset, ledgerType,changeType, amount, availableBefore, null, lockedBefore, lockedAfter, referenceType, null, null, idempotencyKey)),
                Arguments.of("locked_before null", new Ledger.CreateCommand(wallet, asset, ledgerType,changeType, amount, availableBefore, availableAfter, null, lockedAfter, referenceType, null, null, idempotencyKey)),
                Arguments.of("locked_after null", new Ledger.CreateCommand(wallet, asset, ledgerType,changeType, amount, availableBefore, availableAfter, lockedBefore, null, referenceType, null, null, idempotencyKey)),
                Arguments.of("reference_type null", new Ledger.CreateCommand(wallet, asset, ledgerType,changeType, amount, availableBefore, availableAfter, lockedBefore, lockedAfter, null, null, null, idempotencyKey)),
                Arguments.of("idempotency_key null", new Ledger.CreateCommand(wallet, asset, ledgerType,changeType, amount, availableBefore, availableAfter, lockedBefore, lockedAfter, referenceType, null, null, null))
                );
    }

    @Test
    @DisplayName("Negative : 필수 입력값이 blank면 BusinessException을 반환한다.")
    void create_ledger_with_blank_inputs() {
        // given
        idempotencyKey = "";
        Ledger.CreateCommand command = createCommand(wallet, asset, ledgerType, changeType, amount, availableBefore, availableAfter, lockedBefore, lockedAfter, referenceType, referenceId, description, idempotencyKey);

        // when & then
        // idempotency_key blank
        assertThatThrownBy(()-> Ledger.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LEDGER_INPUT);
    }

    @Test
    @DisplayName("Negative : 필수 입력값이 blank면 BusinessException을 반환한다.")
    void create_ledger_with_blank_inputs_2() {
        // given
        idempotencyKey = "   ";
        Ledger.CreateCommand command = createCommand(wallet, asset, ledgerType, changeType, amount, availableBefore, availableAfter, lockedBefore, lockedAfter, referenceType, referenceId, description, idempotencyKey);

        // when & then
        // idempotency_key blank
        assertThatThrownBy(()-> Ledger.create(command))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LEDGER_INPUT);
    }


    // 커맨드 생성 헬퍼
    private Ledger.CreateCommand createCommand(
            Wallet wallet,
            Asset asset,
            LedgerType type,
            ChangeType changeType,
            BigDecimal amount,
            BigDecimal availableBefore,
            BigDecimal availableAfter,
            BigDecimal lockedBefore,
            BigDecimal lockedAfter,
            ReferenceType referenceType,
            Long referenceId,
            String description,
            String idempotencyKey
    ) {
        return new Ledger.CreateCommand(
                wallet, asset, type, changeType, amount, availableBefore, availableAfter, lockedBefore, lockedAfter, referenceType, referenceId, description, idempotencyKey
        );
    }
}
