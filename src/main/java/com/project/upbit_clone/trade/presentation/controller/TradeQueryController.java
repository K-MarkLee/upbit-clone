package com.project.upbit_clone.trade.presentation.controller;

import com.project.upbit_clone.global.presentation.controller.BaseController;
import com.project.upbit_clone.global.presentation.response.ApiResponse;
import com.project.upbit_clone.trade.application.service.TradeQueryService;
import com.project.upbit_clone.trade.presentation.response.TradeQueryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/trades")
@Tag(name = "Trade Query API", description = "체결 조회 API")
public class TradeQueryController extends BaseController {

    private final TradeQueryService tradeQueryService;

    @GetMapping
    @Operation(
            summary = "최근 체결 조회",
            description = "시장 기준 최근 체결 100건을 조회합니다."
    )
    // TODO: 추후 cursor 기반 조회로 변경한다.
    public ResponseEntity<ApiResponse<List<TradeQueryResponse>>> findRecentTrades(
            @RequestParam @NotNull @Positive Long marketId
    ) {
        return ok(tradeQueryService.findRecentTrades(marketId));
    }
}
