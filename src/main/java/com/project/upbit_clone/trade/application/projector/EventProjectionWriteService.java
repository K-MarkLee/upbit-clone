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
import com.project.upbit_clone.trade.infrastructure.persistence.repository.OrderBookProjectionRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.EventType;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.LogType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

@Service
public class EventProjectionWriteService {

    private static final String ORDER_BOOK_PROJECTOR = "order-book-projector";

    private final OrderBookProjectionRepository orderBookProjectionRepository;
    private final ConsumerOffsetRepository consumerOffsetRepository;
    private final JsonMapper jsonMapper;

    public EventProjectionWriteService(
            OrderBookProjectionRepository orderBookProjectionRepository,
            ConsumerOffsetRepository consumerOffsetRepository,
            JsonMapper jsonMapper
    ) {
        this.orderBookProjectionRepository = orderBookProjectionRepository;
        this.consumerOffsetRepository = consumerOffsetRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void project(EventLog event) {
        if (event == null || event.getId() == null) {
            throw new BusinessException(ErrorCode.INVALID_EVENT_PAYLOAD, "event_log_id가 없는 이벤트는 projection 할 수 없습니다.");
        }

        if (event.getEventType() == EventType.ORDER_BOOK_DELTA) {
            projectOrderBookDelta(event);
        }

        consumerOffsetRepository.save(ConsumerOffset.create(
                orderBookProjectorOffsetId(event.getMarketId()),
                event.getId()
        ));
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
        } catch (JacksonException primaryException) {
            try {
                String unwrappedPayload = jsonMapper.readValue(payload, String.class);
                return OrderBookDeltaPayload.from(jsonMapper.readValue(unwrappedPayload, RawOrderBookDeltaPayload.class));
            } catch (JacksonException | IllegalArgumentException | IllegalStateException secondaryException) {
                throw new BusinessException(ErrorCode.INVALID_EVENT_PAYLOAD, "ORDER_BOOK_DELTA payload가 유효하지 않습니다.");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
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
                throw new IllegalStateException("ORDER_BOOK_DELTA payload 필수값이 누락되었습니다.");
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
