package com.project.upbit_clone.trade.presentation.request;

import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class OrderRequest {

    private OrderRequest() {
    }

    public record Place(
            @NotNull Long userId,
            @NotNull Long marketId,
            @NotBlank String clientOrderId,
            @NotNull OrderSide orderSide,
            @NotNull OrderType orderType,
            TimeInForce timeInForce,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount
    ) {
    }

    public record Cancel(
            @NotNull Long userId,
            @NotNull Long marketId,
            @NotBlank String clientOrderId,
            String cancelReason
    ) {
    }
}
