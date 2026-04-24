package com.project.upbit_clone.wallet.application.service;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.wallet.domain.repository.LedgerRepository;
import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import com.project.upbit_clone.wallet.presentation.response.LedgerQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerQueryService {

    private final LedgerRepository ledgerRepository;
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public List<LedgerQueryResponse> findRecentLedgers(Long userId, Long walletId) {
        if (!walletRepository.existsByIdAndUserId(walletId, userId)) {
            throw new BusinessException(ErrorCode.WALLET_NOT_FOUND);
        }
        return ledgerRepository.findTop50ByWalletIdOrderByIdDesc(walletId).stream()
                .map(LedgerQueryResponse::from)
                .toList();
    }
}
