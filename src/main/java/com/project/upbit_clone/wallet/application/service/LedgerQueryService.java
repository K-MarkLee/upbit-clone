package com.project.upbit_clone.wallet.application.service;

import com.project.upbit_clone.wallet.domain.repository.LedgerRepository;
import com.project.upbit_clone.wallet.presentation.response.LedgerQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerQueryService {

    private final LedgerRepository ledgerRepository;

    @Transactional(readOnly = true)
    public List<LedgerQueryResponse> findRecentLedgers(Long walletId) {
        return ledgerRepository.findTop50ByWalletIdOrderByIdDesc(walletId).stream()
                .map(LedgerQueryResponse::from)
                .toList();
    }
}
