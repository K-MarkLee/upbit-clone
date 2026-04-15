package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.presentation.response.MarketQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketQueryService {

    private final MarketRepository marketRepository;

    @Transactional(readOnly = true)
    public List<MarketQueryResponse> findMarkets() {
        return marketRepository.findAllByOrderByMarketCodeAsc().stream()
                .map(MarketQueryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MarketQueryResponse findMarket(Long marketId) {
        return marketRepository.findWithAssetsById(marketId)
                .map(MarketQueryResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.MARKET_NOT_FOUND));
    }
}
