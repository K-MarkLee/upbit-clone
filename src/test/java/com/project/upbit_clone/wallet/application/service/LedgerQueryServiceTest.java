package com.project.upbit_clone.wallet.application.service;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.wallet.domain.model.Ledger;
import com.project.upbit_clone.wallet.domain.model.Wallet;
import com.project.upbit_clone.wallet.domain.repository.LedgerRepository;
import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import com.project.upbit_clone.wallet.domain.vo.ChangeType;
import com.project.upbit_clone.wallet.domain.vo.LedgerType;
import com.project.upbit_clone.wallet.domain.vo.ReferenceType;
import com.project.upbit_clone.wallet.presentation.response.LedgerQueryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerQueryService 단위 테스트")
class LedgerQueryServiceTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private LedgerQueryService ledgerQueryService;

    @Test
    @DisplayName("Happy : userId와 walletId가 일치하면 최근 원장 50건을 조회한다.")
    void find_recent_ledgers_with_owned_wallet() {
        User user = user(1L);
        Asset asset = asset(10L, "BTC");
        Wallet wallet = wallet(100L, user, asset);
        Ledger ledger = ledger(1000L, wallet, asset, "ORDER_UNLOCK");

        when(walletRepository.existsByIdAndUserId(100L, 1L)).thenReturn(true);
        when(ledgerRepository.findTop50ByWalletIdOrderByIdDesc(100L)).thenReturn(List.of(ledger));

        List<LedgerQueryResponse> responses = ledgerQueryService.findRecentLedgers(1L, 100L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().ledgerId()).isEqualTo(1000L);
        assertThat(responses.getFirst().walletId()).isEqualTo(100L);
        assertThat(responses.getFirst().assetSymbol()).isEqualTo("BTC");
        assertThat(responses.getFirst().description()).isEqualTo("ORDER_UNLOCK");
    }

    @Test
    @DisplayName("Negative : 일치하는 지갑이 없으면 WALLET_NOT_FOUND를 반환한다.")
    void find_recent_ledgers_with_missing_wallet() {
        when(walletRepository.existsByIdAndUserId(100L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> ledgerQueryService.findRecentLedgers(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_NOT_FOUND);

        verify(ledgerRepository, never()).findTop50ByWalletIdOrderByIdDesc(100L);
    }

    @Test
    @DisplayName("Negative : 다른 user의 지갑이면 WALLET_NOT_FOUND를 반환한다.")
    void find_recent_ledgers_with_wallet_ownership_mismatch() {
        when(walletRepository.existsByIdAndUserId(100L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> ledgerQueryService.findRecentLedgers(2L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_NOT_FOUND);

        verify(ledgerRepository, never()).findTop50ByWalletIdOrderByIdDesc(100L);
    }

    private static User user(Long id) {
        User user = User.create("user@test.com", "user", EnumStatus.ACTIVE, "pw");
        setField(user, "id", id);
        return user;
    }

    private static Asset asset(Long id, String symbol) {
        Asset asset = Asset.create(symbol, symbol, (byte) 8, EnumStatus.ACTIVE);
        setField(asset, "id", id);
        return asset;
    }

    private static Wallet wallet(Long id, User user, Asset asset) {
        Wallet wallet = Wallet.create(user, asset, BigDecimal.ZERO, BigDecimal.ZERO);
        setField(wallet, "id", id);
        return wallet;
    }

    private static Ledger ledger(Long id, Wallet wallet, Asset asset, String description) {
        Ledger ledger = Ledger.create(new Ledger.CreateCommand(
                wallet,
                asset,
                LedgerType.ORDER_UNLOCK,
                ChangeType.INCREASE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                ReferenceType.ORDER,
                1L,
                description,
                "ledger-key-" + id
        ));
        setField(ledger, "id", id);
        setField(ledger, "createdAt", LocalDateTime.of(2026, 4, 24, 12, 0));
        return ledger;
    }

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("필드 설정 실패: " + fieldName, exception);
            }
        }
        throw new IllegalStateException("필드를 찾을 수 없습니다: " + fieldName);
    }
}
