package com.project.upbit_clone.trade.domain.repository;

import com.project.upbit_clone.trade.domain.model.Trade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    @EntityGraph(attributePaths = {
            "market"
    })
    List<Trade> findTop100ByMarketIdOrderByIdDesc(Long marketId);
}
