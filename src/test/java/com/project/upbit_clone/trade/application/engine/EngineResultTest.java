package com.project.upbit_clone.trade.application.engine;

import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.application.engine.orderbook.PriceLevel;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EngineResult 단위 테스트")
class EngineResultTest {

    private BigDecimal one;
    private BigDecimal two;
    private BigDecimal thousand;
    private BigDecimal twoThousand;
    private List<EngineResult.Fill> emptyFills;
    private List<EngineResult.BookDelta> emptyBookDeltas;

    @BeforeEach
    void setUp() {
        one = new BigDecimal("1");
        two = new BigDecimal("2");
        thousand = new BigDecimal("1000");
        twoThousand = new BigDecimal("2000");
        emptyFills = List.of();
        emptyBookDeltas = List.of();
    }

    @Test
    @DisplayName("Happy : open 팩토리는 OPEN 상태와 기본값을 반환한다.")
    void open_factory_returns_open_status_with_default_values() {
        // when
        EngineResult.PlaceResult result = EngineResult.PlaceResult.open(two);

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
    @DisplayName("Happy : filled 팩토리는 FILLED 상태와 체결 결과를 반환한다.")
    void filled_factory_returns_filled_status_with_execution_values() {
        // given
        EngineResult.Fill fill = new EngineResult.Fill(
                101L,
                thousand,
                one,
                thousand,
                BigDecimal.ZERO
        );
        EngineResult.BookDelta bookDelta = new EngineResult.BookDelta(
                sampleLevelDelta(),
                EngineResult.BookDeltaReason.MATCH_EXECUTED
        );

        // when
        EngineResult.PlaceResult result = EngineResult.PlaceResult.filled(
                one,
                thousand,
                List.of(fill),
                List.of(bookDelta)
        );

        // then
        assertThat(result.takerStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.executedQuantity()).isEqualByComparingTo("1");
        assertThat(result.executedQuoteAmount()).isEqualByComparingTo("1000");
        assertThat(result.remainingQuantity()).isEqualByComparingTo("0");
        assertThat(result.unlockAmount()).isEqualByComparingTo("0");
        assertThat(result.cancelReason()).isNull();
        assertThat(result.fills()).containsExactly(fill);
        assertThat(result.bookDeltas()).containsExactly(bookDelta);
    }

    @Test
    @DisplayName("Happy : PlaceResult는 fills와 bookDeltas를 방어적으로 복사한다.")
    void place_result_defensively_copies_collections() {
        // given
        List<EngineResult.Fill> fills = new ArrayList<>();
        fills.add(new EngineResult.Fill(
                101L,
                thousand,
                one,
                thousand,
                BigDecimal.ZERO
        ));
        List<EngineResult.BookDelta> bookDeltas = new ArrayList<>();
        bookDeltas.add(sampleBookDelta());
        EngineResult.Fill additionalFill = new EngineResult.Fill(
                102L,
                thousand,
                one,
                thousand,
                BigDecimal.ZERO
        );
        EngineResult.BookDelta additionalBookDelta = sampleBookDelta();

        // when
        EngineResult.PlaceResult result = new EngineResult.PlaceResult(
                OrderStatus.OPEN,
                one,
                thousand,
                one,
                BigDecimal.ZERO,
                null,
                fills,
                bookDeltas
        );
        fills.clear();
        bookDeltas.clear();
        List<EngineResult.Fill> copiedFills = result.fills();
        List<EngineResult.BookDelta> copiedBookDeltas = result.bookDeltas();

        // then
        assertThat(result.fills()).hasSize(1);
        assertThat(result.bookDeltas()).hasSize(1);
        assertThatThrownBy(() -> copiedFills.add(additionalFill))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> copiedBookDeltas.add(additionalBookDelta))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Negative : PlaceResult는 음수 잔량과 음수 언락 금액을 허용하지 않는다.")
    void place_result_rejects_negative_remaining_quantity_or_unlock_amount() {
        BigDecimal negativeRemainingQuantity = new BigDecimal("-1");
        BigDecimal negativeUnlockAmount = new BigDecimal("-1");

        assertThatThrownBy(() -> new EngineResult.PlaceResult(
                OrderStatus.CANCELED,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                negativeRemainingQuantity,
                BigDecimal.ZERO,
                EngineResult.CancelReason.IOC_REMAINDER,
                emptyFills,
                emptyBookDeltas
        )).isInstanceOf(EngineException.class)
                .hasMessage("remainingQuantity는 0보다 커야 합니다.");

        assertThatThrownBy(() -> new EngineResult.PlaceResult(
                OrderStatus.CANCELED,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                negativeUnlockAmount,
                EngineResult.CancelReason.IOC_REMAINDER,
                emptyFills,
                emptyBookDeltas
        )).isInstanceOf(EngineException.class)
                .hasMessage("결과 값은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("Negative : Fill과 BookDelta는 필수값을 검증한다.")
    void fill_and_book_delta_validate_required_fields() {
        InMemoryOrderBook.LevelDelta levelDelta = sampleLevelDelta();

        assertThatThrownBy(() -> new EngineResult.Fill(
                101L,
                thousand,
                BigDecimal.ZERO,
                thousand,
                BigDecimal.ZERO
        )).isInstanceOf(EngineException.class)
                .hasMessage("executedQuantity는 0보다 커야 합니다.");

        assertThatThrownBy(() -> new EngineResult.BookDelta(levelDelta, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("reason은 null일 수 없습니다.");
    }

    @Test
    @DisplayName("Negative : PlaceResult는 상태별 불변식을 검증한다.")
    void place_result_validates_status_invariants() {
        BigDecimal remainingQuantity = one;
        EngineResult.CancelReason cancelReason = EngineResult.CancelReason.USER_REQUEST;

        assertThatThrownBy(() -> new EngineResult.PlaceResult(
                OrderStatus.OPEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                remainingQuantity,
                BigDecimal.ZERO,
                cancelReason,
                emptyFills,
                emptyBookDeltas
        )).isInstanceOf(EngineException.class)
                .hasMessage("OPEN 상태에서는 cancelReason이 null이어야 합니다.");

        assertThatThrownBy(() -> new EngineResult.PlaceResult(
                OrderStatus.FILLED,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                emptyFills,
                emptyBookDeltas
        )).isInstanceOf(EngineException.class)
                .hasMessage("FILLED 상태에서는 체결 값(수량, 금액)이 0보다 커야 합니다.");

        assertThatThrownBy(() -> new EngineResult.PlaceResult(
                OrderStatus.CANCELED,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                null,
                emptyFills,
                emptyBookDeltas
        )).isInstanceOf(EngineException.class)
                .hasMessage("CANCELED 상태에서는 cancelReason이 null일 수 없습니다.");
    }

    @Test
    @DisplayName("Negative : PlaceResult는 체결 수치와 fills 합계를 함께 검증한다.")
    void place_result_validates_fill_invariants() {
        List<EngineResult.Fill> mismatchedFills = List.of(new EngineResult.Fill(
                101L,
                thousand,
                one,
                thousand,
                BigDecimal.ZERO
        ));

        assertThatThrownBy(() -> new EngineResult.PlaceResult(
                OrderStatus.FILLED,
                one,
                thousand,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                emptyFills,
                emptyBookDeltas
        )).isInstanceOf(EngineException.class)
                .hasMessage("체결 값이 있으면 fills가 비어 있을 수 없습니다.");

        assertThatThrownBy(() -> new EngineResult.PlaceResult(
                OrderStatus.FILLED,
                two,
                twoThousand,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                mismatchedFills,
                emptyBookDeltas
        )).isInstanceOf(EngineException.class)
                .hasMessage("fills의 quantity 합과 executedQuantity가 일치해야 합니다.");
    }

    private EngineResult.BookDelta sampleBookDelta() {
        return new EngineResult.BookDelta(sampleLevelDelta(), EngineResult.BookDeltaReason.MATCH_EXECUTED);
    }

    private InMemoryOrderBook.LevelDelta sampleLevelDelta() {
        return new InMemoryOrderBook.LevelDelta(
                OrderSide.BID,
                thousand,
                PriceLevel.emptySnapshot(OrderSide.BID, thousand),
                new PriceLevel.Snapshot(OrderSide.BID, thousand, one, 1)
        );
    }
}
