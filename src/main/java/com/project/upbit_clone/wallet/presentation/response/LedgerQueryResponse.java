package com.project.upbit_clone.wallet.presentation.response;

import com.project.upbit_clone.wallet.domain.model.Ledger;
import com.project.upbit_clone.wallet.domain.vo.ChangeType;
import com.project.upbit_clone.wallet.domain.vo.LedgerType;
import com.project.upbit_clone.wallet.domain.vo.ReferenceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LedgerQueryResponse(
        Long ledgerId,
        Long walletId,
        String assetSymbol,
        LedgerType ledgerType,
        ChangeType changeType,
        BigDecimal amount,
        BigDecimal availableBefore,
        BigDecimal availableAfter,
        BigDecimal lockedBefore,
        BigDecimal lockedAfter,
        ReferenceType referenceType,
        Long referenceId,
        String description,
        LocalDateTime createdAt
) {

    public static LedgerQueryResponse from(Ledger ledger) {
        return new LedgerQueryResponse(
                ledger.getId(),
                ledger.getWallet().getId(),
                ledger.getAsset().getSymbol(),
                ledger.getLedgerType(),
                ledger.getChangeType(),
                ledger.getAmount(),
                ledger.getAvailableBefore(),
                ledger.getAvailableAfter(),
                ledger.getLockedBefore(),
                ledger.getLockedAfter(),
                ledger.getReferenceType(),
                ledger.getReferenceId(),
                ledger.getDescription(),
                ledger.getCreatedAt()
        );
    }
}
