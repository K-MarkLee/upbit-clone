package com.project.upbit_clone.trade.domain.repository;

import com.project.upbit_clone.trade.domain.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
}
