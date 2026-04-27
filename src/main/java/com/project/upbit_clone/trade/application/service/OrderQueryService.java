package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.presentation.response.OrderQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<OrderQueryResponse> findOrders(Long userId, Long marketId) {
        if (marketId == null) {
            return orderRepository.findTop10ByUserIdOrderByIdDesc(userId).stream()
                    .map(OrderQueryResponse::from)
                    .toList();
        }
        return orderRepository.findTop10ByUserIdAndMarketIdOrderByIdDesc(userId, marketId).stream()
                .map(OrderQueryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderQueryResponse findOrder(Long userId, String clientOrderId) {
        return orderRepository.findByUserIdAndClientOrderId(userId, clientOrderId)
                .map(OrderQueryResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
