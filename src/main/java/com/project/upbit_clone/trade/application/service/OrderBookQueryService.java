package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.infrastructure.persistence.model.OrderBookProjection;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.OrderBookProjectionRepository;
import com.project.upbit_clone.trade.presentation.response.OrderBookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderBookQueryService {

    private final OrderBookProjectionRepository orderBookProjectionRepository;
    private final MarketRepository marketRepository;

    @Transactional(readOnly = true)
    public OrderBookResponse findOrderBook(Long marketId) {
        validateMarketExists(marketId);
        // TODO: 추후 호가 조회는 cache/snapshot + websocket delta 기반으로 전환해야함.
        List<OrderBookProjection> bids = orderBookProjectionRepository
                .findTop30ByIdMarketIdAndIdSideOrderByIdPriceDesc(marketId, OrderSide.BID);
        List<OrderBookProjection> asks = orderBookProjectionRepository
                .findTop30ByIdMarketIdAndIdSideOrderByIdPriceAsc(marketId, OrderSide.ASK);
        return OrderBookResponse.from(marketId, bids, asks);
    }

    private void validateMarketExists(Long marketId) {
        // TODO: 추후 market metadata cache 기반 검증으로 전환한다.
        if (!marketRepository.existsById(marketId)) {
            throw new BusinessException(ErrorCode.MARKET_NOT_FOUND);
        }
    }
}
