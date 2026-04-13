package com.project.upbit_clone.trade.application.projector;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffset;
import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffsetId;
import com.project.upbit_clone.trade.infrastructure.persistence.model.EventLog;
import com.project.upbit_clone.trade.infrastructure.persistence.model.OrderBookProjection;
import com.project.upbit_clone.trade.infrastructure.persistence.model.OrderBookProjectionId;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.ConsumerOffsetRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.EventLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.OrderBookProjectionRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.EventType;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.LogType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventProjector 단위 테스트")
class EventProjectorTest {

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private OrderBookProjectionRepository orderBookProjectionRepository;

    @Mock
    private ConsumerOffsetRepository consumerOffsetRepository;

    @Captor
    private ArgumentCaptor<OrderBookProjection> projectionCaptor;

    @Captor
    private ArgumentCaptor<ConsumerOffset> offsetCaptor;

    private EventProjector eventProjector;

    @BeforeEach
    void setUp() {
        EventProjectionWriteService eventProjectionWriteService = new EventProjectionWriteService(
                orderBookProjectionRepository,
                consumerOffsetRepository,
                JsonMapper.builder().build()
        );
        eventProjector = new EventProjector(
                eventLogRepository,
                consumerOffsetRepository,
                eventProjectionWriteService
        );
    }

    @Test
    @DisplayName("Happy : ORDER_BOOK_DELTA 이벤트를 projection row로 upsert 한다.")
    void project_order_book_delta_upserts_projection() {
        // given
        EventLog eventLog = eventLog(
                10L,
                EventType.ORDER_BOOK_DELTA,
                """
                        {"reason":"RESTING_ORDER_ADDED","side":"BID","price":"1000","beforeTotalQty":"0","beforeOrderCount":0,"afterTotalQty":"2.5","afterOrderCount":3}
                        """
        );
        OrderBookProjectionId projectionId = new OrderBookProjectionId(100L, OrderSide.BID, new BigDecimal("1000"));

        when(consumerOffsetRepository.findById(offsetId())).thenReturn(Optional.empty());
        when(eventLogRepository.findTop100ByMarketIdAndIdGreaterThanOrderByIdAsc(100L, 0L))
                .thenReturn(List.of(eventLog));
        when(orderBookProjectionRepository.findById(projectionId)).thenReturn(Optional.empty());

        // when
        eventProjector.projectAvailableEvents(100L);

        // then
        verify(orderBookProjectionRepository).save(projectionCaptor.capture());
        assertThat(projectionCaptor.getValue().getId()).isEqualTo(projectionId);
        assertThat(projectionCaptor.getValue().getTotalQty()).isEqualByComparingTo("2.5");
        assertThat(projectionCaptor.getValue().getOrderCount()).isEqualTo(3);

        verify(consumerOffsetRepository).save(offsetCaptor.capture());
        assertThat(offsetCaptor.getValue().getId()).isEqualTo(offsetId());
        assertThat(offsetCaptor.getValue().getLastOffset()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Happy : afterOrderCount가 0이면 projection row를 삭제한다.")
    void project_order_book_delta_deletes_empty_projection_level() {
        // given
        EventLog eventLog = eventLog(
                11L,
                EventType.ORDER_BOOK_DELTA,
                """
                        {"reason":"MATCH_EXECUTED","side":"ASK","price":"2000","beforeTotalQty":"1","beforeOrderCount":1,"afterTotalQty":"0","afterOrderCount":0}
                        """
        );
        OrderBookProjectionId projectionId = new OrderBookProjectionId(100L, OrderSide.ASK, new BigDecimal("2000"));
        OrderBookProjection projection = OrderBookProjection.create(projectionId, BigDecimal.ONE, 1);

        when(consumerOffsetRepository.findById(offsetId())).thenReturn(Optional.empty());
        when(eventLogRepository.findTop100ByMarketIdAndIdGreaterThanOrderByIdAsc(100L, 0L))
                .thenReturn(List.of(eventLog));
        when(orderBookProjectionRepository.findById(projectionId)).thenReturn(Optional.of(projection));

        // when
        eventProjector.projectAvailableEvents(100L);

        // then
        verify(orderBookProjectionRepository).delete(projection);
        verify(orderBookProjectionRepository, never()).save(any(OrderBookProjection.class));
        verify(consumerOffsetRepository).save(offsetCaptor.capture());
        assertThat(offsetCaptor.getValue().getLastOffset()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Happy : 다른 이벤트는 no-op 처리하고 offset만 전진한다.")
    void project_non_order_book_delta_advances_offset_only() {
        // given
        EventLog eventLog = eventLog(
                12L,
                EventType.TRADE_EXECUTED,
                "{\"tradeKey\":\"trade-1\"}"
        );

        when(consumerOffsetRepository.findById(offsetId()))
                .thenReturn(Optional.of(ConsumerOffset.create(offsetId(), 9L)));
        when(eventLogRepository.findTop100ByMarketIdAndIdGreaterThanOrderByIdAsc(100L, 9L))
                .thenReturn(List.of(eventLog));

        // when
        eventProjector.projectAvailableEvents(100L);

        // then
        verify(orderBookProjectionRepository, never()).findById(any(OrderBookProjectionId.class));
        verify(orderBookProjectionRepository, never()).save(any(OrderBookProjection.class));
        verify(consumerOffsetRepository).save(offsetCaptor.capture());
        assertThat(offsetCaptor.getValue().getLastOffset()).isEqualTo(12L);
    }

    @Test
    @DisplayName("Happy : ORDER_BOOK_DELTA payload가 유효하지 않으면 해당 market projector를 blocked 처리한다.")
    void project_invalid_order_book_delta_payload_blocks_market_projector() {
        // given
        EventLog eventLog = eventLog(
                13L,
                EventType.ORDER_BOOK_DELTA,
                """
                        {"reason":"MATCH_EXECUTED","side":"ASK","price":"not-a-number","beforeTotalQty":"1","beforeOrderCount":1,"afterTotalQty":"0","afterOrderCount":0}
                        """
        );

        when(consumerOffsetRepository.findById(offsetId())).thenReturn(Optional.empty());
        when(eventLogRepository.findTop100ByMarketIdAndIdGreaterThanOrderByIdAsc(100L, 0L))
                .thenReturn(List.of(eventLog));

        // when
        Throwable firstThrowable = catchThrowable(() -> eventProjector.projectAvailableEvents(100L));
        Throwable blockedThrowable = catchThrowable(() -> eventProjector.projectAvailableEvents(100L));

        // then
        assertThat(firstThrowable).isInstanceOf(BusinessException.class);
        assertThat(blockedThrowable).isInstanceOf(BusinessException.class);
        BusinessException firstException = (BusinessException) firstThrowable;
        BusinessException blockedException = (BusinessException) blockedThrowable;
        assertThat(firstException.getErrorCode()).isEqualTo(ErrorCode.INVALID_EVENT_PAYLOAD);
        assertThat(blockedException.getErrorCode()).isEqualTo(ErrorCode.PROJECTOR_BLOCKED);
        verify(consumerOffsetRepository, never()).save(any(ConsumerOffset.class));
        verify(orderBookProjectionRepository, never()).save(any(OrderBookProjection.class));
        verify(eventLogRepository).findTop100ByMarketIdAndIdGreaterThanOrderByIdAsc(100L, 0L);
    }

    @Test
    @DisplayName("Happy : batch 중간 이벤트가 실패해도 이전 이벤트의 projection과 offset은 커밋한다.")
    void project_invalid_later_event_keeps_previous_event_progress() {
        // given
        EventLog validEvent = eventLog(
                14L,
                EventType.ORDER_BOOK_DELTA,
                """
                        {"reason":"RESTING_ORDER_ADDED","side":"BID","price":"1000","beforeTotalQty":"0","beforeOrderCount":0,"afterTotalQty":"2.5","afterOrderCount":3}
                        """
        );
        EventLog invalidEvent = eventLog(
                15L,
                EventType.ORDER_BOOK_DELTA,
                """
                        {"reason":"MATCH_EXECUTED","side":"ASK","price":"not-a-number","beforeTotalQty":"1","beforeOrderCount":1,"afterTotalQty":"0","afterOrderCount":0}
                        """
        );
        OrderBookProjectionId projectionId = new OrderBookProjectionId(100L, OrderSide.BID, new BigDecimal("1000"));

        when(consumerOffsetRepository.findById(offsetId())).thenReturn(Optional.empty());
        when(eventLogRepository.findTop100ByMarketIdAndIdGreaterThanOrderByIdAsc(100L, 0L))
                .thenReturn(List.of(validEvent, invalidEvent));
        when(orderBookProjectionRepository.findById(projectionId)).thenReturn(Optional.empty());

        // when
        Throwable throwable = catchThrowable(() -> eventProjector.projectAvailableEvents(100L));

        // then
        assertThat(throwable).isInstanceOf(BusinessException.class);
        BusinessException exception = (BusinessException) throwable;
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_EVENT_PAYLOAD);

        verify(orderBookProjectionRepository).save(projectionCaptor.capture());
        assertThat(projectionCaptor.getValue().getId()).isEqualTo(projectionId);
        assertThat(projectionCaptor.getValue().getTotalQty()).isEqualByComparingTo("2.5");
        assertThat(projectionCaptor.getValue().getOrderCount()).isEqualTo(3);

        verify(consumerOffsetRepository).save(offsetCaptor.capture());
        assertThat(offsetCaptor.getValue().getId()).isEqualTo(offsetId());
        assertThat(offsetCaptor.getValue().getLastOffset()).isEqualTo(14L);
    }

    private ConsumerOffsetId offsetId() {
        return new ConsumerOffsetId(LogType.EVENT, "order-book-projector", "100");
    }

    private EventLog eventLog(Long id, EventType eventType, String payload) {
        EventLog eventLog = EventLog.create(new EventLog.CreateCommand(
                commandLog(),
                "event-" + id,
                eventType,
                100L,
                1L,
                payload
        ));
        setField(eventLog, "id", id);
        return eventLog;
    }

    private CommandLog commandLog() {
        return CommandLog.create(new CommandLog.CreateCommand(
                "command-1",
                CommandType.PLACE_ORDER,
                100L,
                1L,
                "cid-1",
                "{\"command\":\"place\"}",
                "request-hash"
        ));
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException("필드 설정 실패: " + fieldName, exception);
        }
    }
}
