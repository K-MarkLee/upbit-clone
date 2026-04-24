package com.project.upbit_clone.wallet.application.service;

import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import com.project.upbit_clone.wallet.presentation.response.WalletQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletQueryService {

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public List<WalletQueryResponse> findWallets(Long userId) {
        return walletRepository.findAllByUserIdOrderByAssetSymbolAsc(userId).stream()
                .map(WalletQueryResponse::from)
                .toList();
    }
}
