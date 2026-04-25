package com.project.upbit_clone.wallet.presentation.controller;

import com.project.upbit_clone.global.presentation.controller.BaseController;
import com.project.upbit_clone.global.presentation.response.ApiResponse;
import com.project.upbit_clone.wallet.application.service.WalletQueryService;
import com.project.upbit_clone.wallet.presentation.response.WalletQueryResponse;
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
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallet Query API", description = "지갑 조회 API")
public class WalletController extends BaseController {

    private final WalletQueryService walletQueryService;

    @GetMapping
    @Operation(
            summary = "지갑 목록 조회",
            description = "사용자 기준 보유 지갑과 잔고를 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<WalletQueryResponse>>> findWallets(
            @RequestParam @NotNull @Positive Long userId
    ) {
        return ok(walletQueryService.findWallets(userId));
    }
}
