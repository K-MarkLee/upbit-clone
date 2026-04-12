package com.project.upbit_clone.trade.application.projector;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffset;
import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffsetId;
import com.project.upbit_clone.trade.infrastructure.persistence.model.EventLog;
import com.project.upbit_clone.trade.infrastructure.persistence.model.OrderBookProjection;
import com.project.upbit_clone.trade.infrastructure.persistence.model.OrderBookProjectionId;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.ConsumerOffsetRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.EventLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.OrderBookProjectionRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.EventType;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.LogType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class EventProjector {

    private static final String ORDER_BOOK_PROJECTOR = "order-book-projector";

    private final EventLogRepository eventLogRepository;
    private final OrderBookProjectionRepository orderBookProjectionRepository;
    private final ConsumerOffsetRepository consumerOffsetRepository;
    private final JsonMapper jsonMapper;

    public EventProjector(
            EventLogRepository eventLogRepository,
            OrderBookProjectionRepository orderBookProjectionRepository,
            ConsumerOffsetRepository consumerOffsetRepository,
            JsonMapper jsonMapper
    ) {
        this.eventLogRepository = eventLogRepository;
        this.orderBookProjectionRepository = orderBookProjectionRepository;
        this.consumerOffsetRepository = consumerOffsetRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void projectAvailableEvents(Long marketId) {
        Objects.requireNonNull(marketId, "marketId는 null일 수 없습니다.");

        ConsumerOffsetId offsetId = orderBookProjectorOffsetId(marketId);
        Long lastOffset = consumerOffsetRepository.findById(offsetId)
                .map(ConsumerOffset::getLastOffset)
                .orElse(0L);

        List<EventLog> events = eventLogRepository.findByMarketIdAndIdGreaterThanOrderByIdAsc(marketId, lastOffset);
        if (events.isEmpty()) {
            return;
        }

        Long lastProcessedOffset = lastOffset;
        for (EventLog event : events) {
            if (event.getId() == null) {
                throw new BusinessException(ErrorCode.INVALID_EVENT_LOG_INPUT, "event_log_id가 없는 이벤트는 projection 할 수 없습니다.");
            }
            if (event.getEventType() == EventType.ORDER_BOOK_DELTA) {
                projectOrderBookDelta(event);
            }
            lastProcessedOffset = event.getId();
        }

        consumerOffsetRepository.save(ConsumerOffset.create(offsetId, lastProcessedOffset));
    }

    private ConsumerOffsetId orderBookProjectorOffsetId(Long marketId) {
        return new ConsumerOffsetId(LogType.EVENT, ORDER_BOOK_PROJECTOR, String.valueOf(marketId));
    }

    private void projectOrderBookDelta(EventLog event) {
        OrderBookDeltaPayload payload = readOrderBookDeltaPayload(event.getPayload());
        OrderBookProjectionId projectionId = new OrderBookProjectionId(
                event.getMarketId(),
                payload.orderSide(),
                payload.price()
        );

        if (payload.afterOrderCount() == 0) {
            orderBookProjectionRepository.findById(projectionId)
                    .ifPresent(orderBookProjectionRepository::delete);
            return;
        }

        OrderBookProjection projection = orderBookProjectionRepository.findById(projectionId)
                .orElseGet(() -> OrderBookProjection.create(
                        projectionId,
                        payload.afterTotalQty(),
                        payload.afterOrderCount()
                ));
        projection.update(payload.afterTotalQty(), payload.afterOrderCount());
        orderBookProjectionRepository.save(projection);
    }

    private OrderBookDeltaPayload readOrderBookDeltaPayload(String payload) {
        try {
            return OrderBookDeltaPayload.from(jsonMapper.readValue(payload, RawOrderBookDeltaPayload.class));
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_EVENT_PAYLOAD, "ORDER_BOOK_DELTA payload가 유효하지 않습니다.");
        }
    }

    private record RawOrderBookDeltaPayload(
            String side,
            String price,
            String afterTotalQty,
            Integer afterOrderCount
    ) {
    }

    private record OrderBookDeltaPayload(
            OrderSide orderSide,
            BigDecimal price,
            BigDecimal afterTotalQty,
            Integer afterOrderCount
    ) {
        private static OrderBookDeltaPayload from(RawOrderBookDeltaPayload raw) {
            if (raw == null
                    || raw.side() == null
                    || raw.price() == null
                    || raw.afterTotalQty() == null
                    || raw.afterOrderCount() == null) {
                throw new IllegalArgumentException("ORDER_BOOK_DELTA payload 필수값이 누락되었습니다.");
            }
            return new OrderBookDeltaPayload(
                    OrderSide.valueOf(raw.side()),
                    new BigDecimal(raw.price()),
                    new BigDecimal(raw.afterTotalQty()),
                    raw.afterOrderCount()
            );
        }
    }
}
