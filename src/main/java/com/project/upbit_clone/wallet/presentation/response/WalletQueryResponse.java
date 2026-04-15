package com.project.upbit_clone.wallet.presentation.response;

import com.project.upbit_clone.wallet.domain.model.Wallet;

import java.math.BigDecimal;

public record WalletQueryResponse(
        Long walletId,
        String assetSymbol,
        String assetName,
        BigDecimal availableBalance,
        BigDecimal lockedBalance
) {

    public static WalletQueryResponse from(Wallet wallet) {
        return new WalletQueryResponse(
                wallet.getId(),
                wallet.getAsset().getSymbol(),
                wallet.getAsset().getName(),
                wallet.getAvailableBalance(),
                wallet.getLockedBalance()
        );
    }
}
