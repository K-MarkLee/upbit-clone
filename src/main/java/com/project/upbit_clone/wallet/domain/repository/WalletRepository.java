package com.project.upbit_clone.wallet.domain.repository;

import com.project.upbit_clone.wallet.domain.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserIdAndAssetId(Long userId, Long assetId);
}
