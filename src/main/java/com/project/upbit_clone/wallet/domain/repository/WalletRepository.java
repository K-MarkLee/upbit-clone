package com.project.upbit_clone.wallet.domain.repository;

import com.project.upbit_clone.wallet.domain.model.Wallet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserIdAndAssetId(Long userId, Long assetId);

    @EntityGraph(attributePaths = {"user", "asset"})
    List<Wallet> findAllByUserIdInAndAssetIdIn(Collection<Long> userIds, Collection<Long> assetIds);
}
