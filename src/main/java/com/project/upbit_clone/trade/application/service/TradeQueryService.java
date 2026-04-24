package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.domain.repository.TradeRepository;
import com.project.upbit_clone.trade.presentation.response.TradeQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeQueryService {

    private final TradeRepository tradeRepository;
    private final MarketRepository marketRepository;

    @Transactional(readOnly = true)
    public List<TradeQueryResponse> findRecentTrades(Long marketId) {
        validateMarketExists(marketId);
        return tradeRepository.findTop100ByMarketIdOrderByIdDesc(marketId)
                .stream()
                .map(TradeQueryResponse::from)
                .toList();
    }

    private void validateMarketExists(Long marketId) {
        // TODO: 추후 market metadata cache 기반 검증으로 전환한다.
        if (!marketRepository.existsById(marketId)) {
            throw new BusinessException(ErrorCode.MARKET_NOT_FOUND);
        }
    }
}
