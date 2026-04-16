package com.project.upbit_clone.wallet.presentation.controller;

import com.project.upbit_clone.global.presentation.controller.BaseController;
import com.project.upbit_clone.global.presentation.response.ApiResponse;
import com.project.upbit_clone.wallet.application.service.LedgerQueryService;
import com.project.upbit_clone.wallet.presentation.response.LedgerQueryResponse;
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

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ledgers")
@Tag(name = "Ledger Query API", description = "원장 조회 API")
public class LedgerController extends BaseController {

    private final LedgerQueryService ledgerQueryService;

    @GetMapping
    @Operation(
            summary = "원장 조회",
            description = "지갑 기준 최근 원장 변동 내역 50건을 최신순으로 조회합니다."
    )
    // TODO: 추후 cursor 기반 조회로 변경한다.
    public ResponseEntity<ApiResponse<List<LedgerQueryResponse>>> findLedgers(
            @RequestParam @NotNull Long walletId
    ) {
        return ok(ledgerQueryService.findRecentLedgers(walletId));
    }
}
