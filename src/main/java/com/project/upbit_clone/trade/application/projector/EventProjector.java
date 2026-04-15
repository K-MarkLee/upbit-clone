package com.project.upbit_clone.trade.application.projector;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffset;
import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffsetId;
import com.project.upbit_clone.trade.infrastructure.persistence.model.EventLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.ConsumerOffsetRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.EventLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.EventType;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventProjector {

    private static final Logger log = LoggerFactory.getLogger(EventProjector.class);
    private static final String ORDER_BOOK_PROJECTOR = "order-book-projector";
    private static final int PAYLOAD_LOG_LIMIT = 500;
    private final Map<Long, ProjectorBlockReason> blockedMarkets = new ConcurrentHashMap<>();

    private final EventLogRepository eventLogRepository;
    private final ConsumerOffsetRepository consumerOffsetRepository;
    private final EventProjectionWriteService eventProjectionWriteService;

    public EventProjector(
            EventLogRepository eventLogRepository,
            ConsumerOffsetRepository consumerOffsetRepository,
            EventProjectionWriteService eventProjectionWriteService
    ) {
        this.eventLogRepository = eventLogRepository;
        this.consumerOffsetRepository = consumerOffsetRepository;
        this.eventProjectionWriteService = eventProjectionWriteService;
    }

    public void projectAvailableEvents(Long marketId) {
        Objects.requireNonNull(marketId, "marketId는 null일 수 없습니다.");

        ProjectorBlockReason blockReason = blockedMarkets.get(marketId);
        if (blockReason != null) {
            log.warn(
                    "Order book projector가 blocked 상태라 실행하지 않습니다. marketId={}, eventLogId={}, eventType={}, errorCode={}, message={}",
                    marketId,
                    blockReason.eventLogId(),
                    blockReason.eventType(),
                    blockReason.errorCode(),
                    blockReason.message()
            );
            throw new BusinessException(ErrorCode.PROJECTOR_BLOCKED, marketId);
        }

        ConsumerOffsetId offsetId = orderBookProjectorOffsetId(marketId);
        Long lastOffset = consumerOffsetRepository.findById(offsetId)
                .map(ConsumerOffset::getLastOffset)
                .orElse(0L);

        List<EventLog> events = eventLogRepository.findTop100ByMarketIdAndIdGreaterThanOrderByIdAsc(marketId, lastOffset);
        if (events.isEmpty()) {
            return;
        }

        // TODO : 추후 분리시 배치 처리 정책 수정 필요. 현재 100개씩만.
        for (EventLog event : events) {
            try {
                eventProjectionWriteService.project(event);
            } catch (BusinessException exception) {
                blockMarket(marketId, event, exception);
                throw exception;
            }
        }
    }

    private void blockMarket(Long marketId, EventLog event, BusinessException exception) {
        ProjectorBlockReason blockReason = new ProjectorBlockReason(
                event.getId(),
                event.getEventType(),
                exception.getErrorCode(),
                exception.getMessage(),
                payloadForLog(event.getPayload())
        );
        blockedMarkets.put(marketId, blockReason);
        log.error(
                "Order book projector가 blocked 상태로 전환되었습니다. marketId={}, eventLogId={}, eventType={}, errorCode={}, message={}, payload={}",
                marketId,
                blockReason.eventLogId(),
                blockReason.eventType(),
                blockReason.errorCode(),
                blockReason.message(),
                blockReason.payload(),
                exception
        );
    }

    private String payloadForLog(String payload) {
        if (payload == null || payload.length() <= PAYLOAD_LOG_LIMIT) {
            return payload;
        }
        return payload.substring(0, PAYLOAD_LOG_LIMIT) + "...";
    }

    private ConsumerOffsetId orderBookProjectorOffsetId(Long marketId) {
        return new ConsumerOffsetId(LogType.EVENT, ORDER_BOOK_PROJECTOR, String.valueOf(marketId));
    }

    private record ProjectorBlockReason(
            Long eventLogId,
            EventType eventType,
            ErrorCode errorCode,
            String message,
            String payload
    ) {
    }
}
