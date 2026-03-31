package com.project.upbit_clone.trade.application.engine;

import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.application.engine.orderbook.PriceLevel;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EngineResult 단위 테스트")
class EngineResultTest {

    @Test
    @DisplayName("Happy : open 팩토리는 OPEN 상태와 기본값을 반환한다.")
    void open_factory_returns_open_status_with_default_values() {
        // when
        EngineResult.PlaceResult result = EngineResult.PlaceResult.open(new BigDecimal("2"));

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(result.executedQuantity()).isEqualByComparingTo("0");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("0");
        assertThat(result.remainingQuantity()).isEqualByComparingTo("2");
        assertThat(result.unlockAmount()).isEqualByComparingTo("0");
        assertThat(result.cancelReason()).isNull();
        assertThat(result.fills()).isEmpty();
        assertThat(result.bookDeltas()).isEmpty();
    }

    @Test
    @DisplayName("Happy : PlaceResult는 fills와 bookDeltas를 방어적으로 복사한다.")
    void place_result_defensively_copies_collections() {
        // given
        List<EngineResult.Fill> fills = new ArrayList<>();
        fills.add(new EngineResult.Fill(
                101L,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                new BigDecimal("1000"),
                BigDecimal.ZERO
        ));
        List<EngineResult.BookDelta> bookDeltas = new ArrayList<>();
        bookDeltas.add(sampleBookDelta());

        // when
        EngineResult.PlaceResult result = new EngineResult.PlaceResult(
                OrderStatus.OPEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1"),
                BigDecimal.ZERO,
                null,
                fills,
                bookDeltas
        );
        fills.clear();
        bookDeltas.clear();

        // then
        assertThat(result.fills()).hasSize(1);
        assertThat(result.bookDeltas()).hasSize(1);
        assertThatThrownBy(() -> result.fills().add(new EngineResult.Fill(
                102L,
                new BigDecimal("1000"),
                new BigDecimal("1"),
                new BigDecimal("1000"),
                BigDecimal.ZERO
        ))).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.bookDeltas().add(sampleBookDelta()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Negative : PlaceResult는 음수 잔량과 음수 언락 금액을 허용하지 않는다.")
    void place_result_rejects_negative_remaining_quantity_or_unlock_amount() {
        assertThatThrownBy(() -> new EngineResult.PlaceResult(
                OrderStatus.CANCELED,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("-1"),
                BigDecimal.ZERO,
                "IOC_REMAINDER",
                List.of(),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("엔진 결과 잔량은 0 미만일 수 없습니다.");

        assertThatThrownBy(() -> new EngineResult.PlaceResult(
                OrderStatus.CANCELED,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                new BigDecimal("-1"),
                "IOC_REMAINDER",
                List.of(),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("엔진 결과 금액은 0 미만일 수 없습니다.");
    }

    @Test
    @DisplayName("Negative : Fill과 BookDelta는 필수값을 검증한다.")
    void fill_and_book_delta_validate_required_fields() {
        assertThatThrownBy(() -> new EngineResult.Fill(
                101L,
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                new BigDecimal("1000"),
                BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("체결 수량은 0보다 커야 합니다.");

        assertThatThrownBy(() -> new EngineResult.BookDelta(sampleLevelDelta(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("reason은 null일 수 없습니다.");
    }

    private EngineResult.BookDelta sampleBookDelta() {
        return new EngineResult.BookDelta(sampleLevelDelta(), EngineResult.BookDeltaReason.MATCH_EXECUTED);
    }

    private InMemoryOrderBook.LevelDelta sampleLevelDelta() {
        BigDecimal price = new BigDecimal("1000");
        return new InMemoryOrderBook.LevelDelta(
                OrderSide.BID,
                price,
                PriceLevel.emptySnapshot(OrderSide.BID, price),
                new PriceLevel.Snapshot(OrderSide.BID, price, new BigDecimal("1"), 1)
        );
    }
}
