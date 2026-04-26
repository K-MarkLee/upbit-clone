package com.project.upbit_clone.trade.presentation.request;

import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class OrderRequest {

    private OrderRequest() {
    }

    public record Place(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long marketId,
            @NotBlank
            @Size(max = 150, message = "client order id는 150자 보다 클 수 없습니다.")
            String clientOrderId,
            @NotNull OrderSide orderSide,
            @NotNull OrderType orderType,
            TimeInForce timeInForce,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount
    ) {
    }

    public record Cancel(
            @NotNull @Positive Long userId,
            @NotBlank
            @Size(max = 150, message = "client order id는 150자 보다 클 수 없습니다.")
            String clientOrderId
    ) {
    }
}
