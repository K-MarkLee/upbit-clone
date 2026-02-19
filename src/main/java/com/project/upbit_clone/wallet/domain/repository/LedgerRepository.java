package com.project.upbit_clone.wallet.domain.repository;

import com.project.upbit_clone.wallet.domain.model.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {
}
