package com.project.upbit_clone.trade.presentation.controller;

import com.project.upbit_clone.global.presentation.controller.BaseController;
import com.project.upbit_clone.global.presentation.response.ApiResponse;
import com.project.upbit_clone.trade.application.ingress.CancelOrder;
import com.project.upbit_clone.trade.application.ingress.CommandAck;
import com.project.upbit_clone.trade.application.ingress.PlaceOrder;
import com.project.upbit_clone.trade.presentation.request.OrderRequest;
import com.project.upbit_clone.trade.presentation.response.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(name = "Order API", description = "주문 API")
public class OrderController extends BaseController {

    private final PlaceOrder placeOrder;
    private final CancelOrder cancelOrder;

    @PostMapping
    @Operation(
            summary = "주문 생성 접수",
            description = "주문 생성 요청을 검증한 뒤 command_log에 기록하고 ACK를 반환합니다."
    )
    public ResponseEntity<ApiResponse<OrderResponse>> place(@Valid @RequestBody OrderRequest.Place request) {
        CommandAck ack = placeOrder.handle(new PlaceOrder.Command(
                request.userId(),
                request.marketId(),
                request.clientOrderId(),
                request.orderSide(),
                request.orderType(),
                request.timeInForce(),
                request.price(),
                request.quantity(),
                request.quoteAmount()
        ));
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(OrderResponse.from(ack)));
    }

    @PostMapping("/cancel")
    @Operation(
            summary = "주문 취소 접수",
            description = "주문 취소 요청을 검증한 뒤 command_log에 기록하고 ACK를 반환합니다."
    )
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@Valid @RequestBody OrderRequest.Cancel request) {
        CommandAck ack = cancelOrder.handle(new CancelOrder.Command(
                request.userId(),
                request.marketId(),
                request.clientOrderId(),
                request.cancelReason()
        ));
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(OrderResponse.from(ack)));
    }
}
