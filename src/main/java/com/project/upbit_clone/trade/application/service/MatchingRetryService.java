package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

@Service
@RequiredArgsConstructor
public class MatchingRetryService {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_NANOS = 5_000_000L; // 5ms

    private final MatchingService matchingService;
    private final OrderRepository orderRepository;

    // 낙관락/락획득 충돌이 발생하면 OPEN 주문에 한해 짧은 백오프로 재시도한다.
    public void matchWithRetry(Long orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");

        for (int attempt = 1; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                matchingService.match(orderId);
                return;
            } catch (OptimisticLockingFailureException | PessimisticLockingFailureException exception) {
                if (!isOpenOrder(orderId)) {
                    throw exception;
                }
                backoff(attempt);
            }
        }

        // 마지막 시도는 예외를 그대로 전달한다.
        matchingService.match(orderId);
    }

    private boolean isOpenOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(order -> order.getStatus() == OrderStatus.OPEN)
                .orElse(false);
    }

    private void backoff(int attempt) {
        long backoffNanos = BASE_BACKOFF_NANOS * (1L << (attempt - 1));
        LockSupport.parkNanos(backoffNanos);
    }
}
