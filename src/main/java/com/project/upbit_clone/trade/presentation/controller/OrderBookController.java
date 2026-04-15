package com.project.upbit_clone.trade.presentation.controller;

import com.project.upbit_clone.global.presentation.controller.BaseController;
import com.project.upbit_clone.global.presentation.response.ApiResponse;
import com.project.upbit_clone.trade.application.service.OrderBookQueryService;
import com.project.upbit_clone.trade.presentation.response.OrderBookResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/order-books")
@Tag(name = "Order Book Query API", description = "호가 조회 API")
public class OrderBookController extends BaseController {

    private final OrderBookQueryService orderBookQueryService;

    @GetMapping
    @Operation(
            summary = "호가 조회",
            description = "order_book_projection 기준으로 매수/매도 각 30레벨을 조회합니다."
    )
    public ResponseEntity<ApiResponse<OrderBookResponse>> findOrderBook(
            @RequestParam @NotNull Long marketId
    ) {
        return ok(orderBookQueryService.findOrderBook(marketId));
    }
}
