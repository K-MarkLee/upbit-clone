package com.project.upbit_clone.trade.domain.repository;

import com.project.upbit_clone.trade.domain.model.Market;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {

    @EntityGraph(attributePaths = {"baseAsset", "quoteAsset"})
    Optional<Market> findWithAssetsById(Long marketId);
}
