package com.project.upbit_clone.trade.presentation.controller;

import com.project.upbit_clone.global.presentation.controller.BaseController;
import com.project.upbit_clone.global.presentation.response.ApiResponse;
import com.project.upbit_clone.trade.application.service.MarketQueryService;
import com.project.upbit_clone.trade.presentation.response.MarketQueryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/markets")
@Tag(name = "Market Query API", description = "시장 조회 API")
public class MarketController extends BaseController {

    private final MarketQueryService marketQueryService;

    @GetMapping
    @Operation(
            summary = "시장 목록 조회",
            description = "시장 목록을 marketCode 오름차순으로 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<MarketQueryResponse>>> findMarkets() {
        return ok(marketQueryService.findMarkets());
    }

    @GetMapping("/{marketId}")
    @Operation(
            summary = "시장 상세 조회",
            description = "시장 ID 기준으로 시장 상세 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<MarketQueryResponse>> findMarket(
            @PathVariable @NotNull Long marketId
    ) {
        return ok(marketQueryService.findMarket(marketId));
    }
}
