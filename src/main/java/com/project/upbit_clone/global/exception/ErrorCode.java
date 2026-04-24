package com.project.upbit_clone.global.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public enum ErrorCode {
    // event projector
    PROJECTOR_BLOCKED("O01", "해당 market의 프로젝터는 blocked 상태입니다.", HttpStatus.CONFLICT),

    // in memory order book
    INVALID_ORDER_BOOK_INPUT("M01", "유효하지 않은 오더북 입력입니다. ", HttpStatus.BAD_REQUEST),

    // order book projection
    INVALID_ORDER_BOOK_PROJECTION_INPUT("L01", "유효하지 않은 오더북 프로젝션 입력입니다.", HttpStatus.BAD_REQUEST),
    NEGATIVE_ORDER_COUNT_NOT_ALLOWED("L02", "주문 카운트는 0 미만일 수 없습니다.", HttpStatus.BAD_REQUEST),

    // event log
    INVALID_EVENT_LOG_INPUT("K01", "유효하지 않은 이벤트 로그 입력입니다.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_PAYLOAD("K02", "유효하지 않은 이벤트 payload 입니다.", HttpStatus.BAD_REQUEST),

    // consumer offset
    INVALID_CONSUMER_OFFSET_INPUT("J01", "유효하지 않은 소비자 오프셋 입력입니다.", HttpStatus.BAD_REQUEST),
    NEGATIVE_OFFSET_NOT_ALLOWED("J02", "오프셋은 0 미만일 수 없습니다.", HttpStatus.BAD_REQUEST),

    //command log
    INVALID_COMMAND_LOG_INPUT("I01", "유효하지 않은 커맨드 로그 입력입니다.", HttpStatus.BAD_REQUEST),
    INVALID_COMMAND_TYPE("I02", "지원하지 않는 커맨드 타입입니다.", HttpStatus.BAD_REQUEST),

    //common
    VALIDATION_ERROR("H01", "요청값이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER_TYPE("H02", "요청 파라미터 타입이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("H03", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    //wallet
    INVALID_WALLET_INPUT("G01", "유효하지 않은 지갑 입력입니다.", HttpStatus.BAD_REQUEST),
    QUOTE_WALLET_NOT_FOUND("G02", "Quote 지갑을 찾을 수 없습니다." , HttpStatus.BAD_REQUEST),
    INSUFFICIENT_AVAILABLE_BALANCE("G03", "가용 잔고가 부족합니다.", HttpStatus.BAD_REQUEST),
    BASE_WALLET_NOT_FOUND("G04", "Base 지갑을 찾을 수 없습니다." , HttpStatus.BAD_REQUEST),
    INSUFFICIENT_LOCKED_BALANCE("G05", "잠금 잔고가 부족합니다." , HttpStatus.BAD_REQUEST),
    WALLET_NOT_FOUND("G06", "일치하는 지갑을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    //asset
    INVALID_ASSET_INPUT("F01", "유효하지 않은 자산 입력입니다.", HttpStatus.BAD_REQUEST),
    INVALID_ASSET_DECIMALS("F02", "자산의 메타데이터가 비정상입니다." , HttpStatus.INTERNAL_SERVER_ERROR),

    //ledger
    INVALID_LEDGER_INPUT("E01", "유효하지 않은 원장 입력입니다.", HttpStatus.BAD_REQUEST),
    ASSET_NOT_MATCHED("E02", "지갑과 원장의 자산이 동일하지 않습니다.", HttpStatus.BAD_REQUEST),

    //order
    INVALID_ORDER_INPUT("D01", "유효하지 않은 주문 입력입니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_TIME_IN_FORCE("D02", "FOK는 현재 지원하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_LIMIT_BID_INPUT("D03", "LIMIT-BID 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_LIMIT_ASK_INPUT("D04", "LIMIT-ASK 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_MARKET_BID_INPUT("D05", "MARKET-BID 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_MARKET_ASK_INPUT("D06", "MARKET-ASK 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MISSING_ORDER_REQUIRED_VALUE("D07","주문에 필요한 필수값이 누락되어 있습니다.", HttpStatus.BAD_REQUEST),
    LOCK_AMOUNT_TOO_LOW("D08", "락 계산 금액이 자산 최소 단위보다 작습니다." , HttpStatus.BAD_REQUEST),
    INVALID_ORDER_SIDE("D09", "유효하지 않은 주문 방향입니다", HttpStatus.BAD_REQUEST),
    ORDER_NOT_OPEN("D10", "현재 상태에서는 주문을 처리할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND("D11", "일치하는 주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ORDER_ACCESS_DENIED("D12", "주문 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    IDEMPOTENCY_CONFLICT("D13", "동일 멱등 키로 서로 다른 요청은 허용되지 않습니다.", HttpStatus.CONFLICT),
    ORDER_OVER_EXECUTED("D14", "체결 수량은 주문 수량을 초과할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ORDER_OVER_SPENT("D15", "체결 금액은 주문 금액을 초과할 수 없습니다.", HttpStatus.BAD_REQUEST),

    //market
    DIFFERENT_ASSET_REQUIRED("C01", "서로 다른 자산이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_MARKET_INPUT("C02", "유효하지 않은 시장 입력입니다.", HttpStatus.BAD_REQUEST),
    MARKET_NOT_FOUND("C03", "일치하는 시장을 찾을 수 없습니다." , HttpStatus.NOT_FOUND),
    NOT_ALLOWED_MARKET_STATUS("C04", "유효하지 않은 시장 상태입니다.", HttpStatus.BAD_REQUEST),

    //trade
    SELF_TRADE_NOT_ALLOWED("B01", "판매자와 구매자는 같을 수 없습니다.", HttpStatus.BAD_REQUEST),
    TRADE_MARKET_NOT_MATCHED("B02", "거래 마켓과 주문 마켓은 같아야 합니다.", HttpStatus.BAD_REQUEST),
    TRADE_PRICE_MUST_BE_MAKER_PRICE("B03", "체결 가격은 메이커 주문 가격이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_TRADE_INPUT("B04", "유효하지 않은 거래 입력입니다.", HttpStatus.BAD_REQUEST),
    ORDER_SIDE_NOT_MATCHED("B05", "거래 방향이 일치해야 합니다.", HttpStatus.BAD_REQUEST),


    //user
    USER_NAME_REQUIRED("A01", "사용자 이름은 필수입니다.", HttpStatus.BAD_REQUEST),
    INVALID_USER_INPUT("A02", "유효하지 않은 사용자 입력입니다.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("A03", "일치하는 사용자를 찾을 수 없습니다." , HttpStatus.NOT_FOUND),
    NOT_ALLOWED_USER_STATUS("A04", "유효하지 않은 사용자 상태입니다." , HttpStatus.BAD_REQUEST);


    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
