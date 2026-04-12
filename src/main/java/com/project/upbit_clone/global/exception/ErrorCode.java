package com.project.upbit_clone.global.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public enum ErrorCode {
    // in memory order book
    INVALID_ORDER_BOOK_INPUT("M01", "유효하지 않은 오더북 입력입니다. ", HttpStatus.BAD_REQUEST),

    // order book projection
    INVALID_ORDER_BOOK_PROJECTION_INPUT("L01", "유효하지 않은 오더북 프로젝션 입력입니다.", HttpStatus.BAD_REQUEST),
    NEGATIVE_ORDER_COUNT_NOT_ALLOWED("L02", "주문 카운트는 0 미만일 수 없습니다.", HttpStatus.BAD_REQUEST),

    // event log
    INVALID_EVENT_LOG_INPUT("K01", "유효하지 않은 이벤트 로그 입력입니다.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_PAYLOAD("K01", "유효하지 않은 이벤트 payload 입니다.", HttpStatus.BAD_REQUEST),

    // consumer offset
    INVALID_CONSUMER_OFFSET_INPUT("J01", "유효하지 않은 소비자 오프셋 입력입니다.", HttpStatus.BAD_REQUEST),
    NEGATIVE_OFFSET_NOT_ALLOWED("J02", "오프셋은 0 미만일 수 없습니다.", HttpStatus.BAD_REQUEST),

    //command log
    INVALID_COMMAND_LOG_INPUT("I01", "유효하지 않은 커맨드 로그 입력입니다.", HttpStatus.BAD_REQUEST),
    INVALID_COMMAND_TYPE("I02", "지원하지 않는 커맨드 타입입니다.", HttpStatus.BAD_REQUEST),

    //common
    VALIDATION_ERROR("H001", "요청값이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER_TYPE("H002", "요청 파라미터 타입이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("H003", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    //wallet
    INVALID_WALLET_INPUT("G001", "유효하지 않은 지갑 입력입니다.", HttpStatus.BAD_REQUEST),
    QUOTE_WALLET_NOT_FOUND("G002", "Quote 지갑을 찾을 수 없습니다." , HttpStatus.BAD_REQUEST),
    INSUFFICIENT_AVAILABLE_BALANCE("G003", "가용 잔고가 부족합니다.", HttpStatus.BAD_REQUEST),
    BASE_WALLET_NOT_FOUND("G004", "Base 지갑을 찾을 수 없습니다." , HttpStatus.BAD_REQUEST),
    INSUFFICIENT_LOCKED_BALANCE("G005", "잠금 잔고가 부족합니다." , HttpStatus.BAD_REQUEST),

    //asset
    INVALID_ASSET_INPUT("F001", "유효하지 않은 자산 입력입니다.", HttpStatus.BAD_REQUEST),
    INVALID_ASSET_DECIMALS("F002", "자산의 메타데이터가 비정상입니다." , HttpStatus.INTERNAL_SERVER_ERROR),

    //ledger
    INVALID_LEDGER_INPUT("E001", "유효하지 않은 원장 입력입니다.", HttpStatus.BAD_REQUEST),
    ASSET_NOT_MATCHED("E002", "지갑과 원장의 자산이 동일하지 않습니다.", HttpStatus.BAD_REQUEST),

    //order
    INVALID_ORDER_INPUT("D001", "유효하지 않은 주문 입력입니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_TIME_IN_FORCE("D002", "FOK는 현재 지원하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_LIMIT_BID_INPUT("D003", "LIMIT-BID 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_LIMIT_ASK_INPUT("D004", "LIMIT-ASK 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_MARKET_BID_INPUT("D005", "MARKET-BID 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_MARKET_ASK_INPUT("D006", "MARKET-ASK 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MISSING_ORDER_REQUIRED_VALUE("D007","주문에 필요한 필수값이 누락되어 있습니다.", HttpStatus.BAD_REQUEST),
    LOCK_AMOUNT_TOO_LOW("D008", "락 계산 금액이 자산 최소 단위보다 작습니다." , HttpStatus.BAD_REQUEST),
    INVALID_ORDER_SIDE("D009", "유효하지 않은 주문 방향입니다", HttpStatus.BAD_REQUEST),
    ORDER_NOT_OPEN("D0010", "현재 상태에서는 주문을 처리할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND("D011", "일치하는 주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ORDER_ACCESS_DENIED("D012", "주문 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    IDEMPOTENCY_CONFLICT("D013", "동일 멱등 키로 서로 다른 요청은 허용되지 않습니다.", HttpStatus.CONFLICT),

    //market
    DIFFERENT_ASSET_REQUIRED("C001", "서로 다른 자산이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_MARKET_INPUT("C002", "유효하지 않은 시장 입력입니다.", HttpStatus.BAD_REQUEST),
    MARKET_NOT_FOUND("C003", "일치하는 시장을 찾을 수 없습니다." , HttpStatus.NOT_FOUND),
    NOT_ALLOWED_MARKET_STATUS("C004", "유효하지 않은 시장 상태입니다.", HttpStatus.BAD_REQUEST),

    //trade
    SELF_TRADE_NOT_ALLOWED("B001", "판매자와 구매자는 같을 수 없습니다.", HttpStatus.BAD_REQUEST),
    TRADE_MARKET_NOT_MATCHED("B002", "거래 마켓과 주문 마켓은 같아야 합니다.", HttpStatus.BAD_REQUEST),
    TRADE_PRICE_MUST_BE_MAKER_PRICE("B003", "체결 가격은 메이커 주문 가격이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_TRADE_INPUT("B004", "유효하지 않은 거래 입력입니다.", HttpStatus.BAD_REQUEST),
    ORDER_SIDE_NOT_MATCHED("B005", "거래 방향이 일치해야 합니다.", HttpStatus.BAD_REQUEST),


    //user
    USER_NAME_REQUIRED("A001", "사용자 이름은 필수입니다.", HttpStatus.BAD_REQUEST),
    INVALID_USER_INPUT("A002", "유효하지 않은 사용자 입력입니다.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("A003", "일치하는 사용자를 찾을 수 없습니다." , HttpStatus.NOT_FOUND),
    NOT_ALLOWED_USER_STATUS("A004", "유효하지 않은 사용자 상태입니다." , HttpStatus.BAD_REQUEST);


    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
