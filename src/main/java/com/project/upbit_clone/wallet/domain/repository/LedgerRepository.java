package com.project.upbit_clone.wallet.domain.repository;

import com.project.upbit_clone.wallet.domain.model.Ledger;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    @EntityGraph(attributePaths = {"wallet", "asset"})
    List<Ledger> findTop50ByWalletIdOrderByIdDesc(Long walletId);
}
