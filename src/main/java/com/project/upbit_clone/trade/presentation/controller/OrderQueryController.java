package com.project.upbit_clone.trade.presentation.controller;

import com.project.upbit_clone.global.presentation.controller.BaseController;
import com.project.upbit_clone.global.presentation.response.ApiResponse;
import com.project.upbit_clone.trade.application.service.OrderQueryService;
import com.project.upbit_clone.trade.presentation.response.OrderQueryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Query API", description = "주문 조회 API")
public class OrderQueryController extends BaseController {

    private final OrderQueryService orderQueryService;

    @GetMapping
    @Operation(
            summary = "주문 목록 조회",
            description = "사용자 기준 주문 목록을 조회합니다. marketId를 전달하면 해당 시장 주문만 반환합니다."
    )
    public ResponseEntity<ApiResponse<List<OrderQueryResponse>>> findOrders(
            @RequestParam @NotNull @Positive Long userId,
            @RequestParam(required = false) @Positive Long marketId
    ) {
        return ok(orderQueryService.findOrders(userId, marketId));
    }

    @GetMapping("/detail")
    @Operation(
            summary = "주문 단건 조회",
            description = "외부 식별자인 userId, clientOrderId 조합으로 주문을 조회합니다."
    )
    public ResponseEntity<ApiResponse<OrderQueryResponse>> findOrder(
            @RequestParam @NotNull @Positive Long userId,
            @RequestParam @NotBlank String clientOrderId
    ) {
        return ok(orderQueryService.findOrder(userId, clientOrderId));
    }
}
