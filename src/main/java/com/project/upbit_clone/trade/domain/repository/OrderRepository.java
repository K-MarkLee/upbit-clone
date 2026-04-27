package com.project.upbit_clone.trade.domain.repository;

import com.project.upbit_clone.trade.domain.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "market", "market.baseAsset", "market.quoteAsset"})
    Optional<Order> findByUserIdAndClientOrderIdAndMarketId(
            Long userId,
            String clientOrderId,
            Long marketId
    );

    @EntityGraph(attributePaths = {"user", "market", "market.baseAsset", "market.quoteAsset"})
    Optional<Order> findByUserIdAndClientOrderId(Long userId, String clientOrderId);

    @EntityGraph(attributePaths = {"user", "market", "market.baseAsset", "market.quoteAsset"})
    Optional<Order> findByOrderKey(String orderKey);

    @EntityGraph(attributePaths = {"user", "market", "market.baseAsset", "market.quoteAsset"})
    List<Order> findAllByOrderKeyIn(Collection<String> orderKeys);

    // TODO : 추후 cursor pagination추가
    @EntityGraph(attributePaths = {"user", "market", "market.baseAsset", "market.quoteAsset"})
    List<Order> findTop10ByUserIdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "market", "market.baseAsset", "market.quoteAsset"})
    List<Order> findTop10ByUserIdAndMarketIdOrderByIdDesc(Long userId, Long marketId);
}
