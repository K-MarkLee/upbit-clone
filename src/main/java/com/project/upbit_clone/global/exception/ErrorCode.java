package com.project.upbit_clone.global.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public enum ErrorCode {
    //market
    DIFFERENT_ASSET_REQUIRED("C001", "서로 다른 자산이어야 합니다.", HttpStatus.BAD_REQUEST),

    //trade
    SELF_TRADE_NOT_ALLOWED("B001", "판매자와 구매자는 같을 수 없습니다.", HttpStatus.BAD_REQUEST),
    TRADE_MARKET_MISMATCH("B002", "체결 마켓과 주문 마켓은 같아야 합니다.", HttpStatus.BAD_REQUEST),
    TRADE_PRICE_MUST_BE_MAKER_PRICE("B003", "체결 가격은 메이커 주문 가격이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_TRADE_INPUT("B004", "유효하지 않은 체결 입력입니다.", HttpStatus.BAD_REQUEST),

    //order
    INVALID_ORDER_INPUT("D001", "유효하지 않은 주문 입력입니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_TIME_IN_FORCE("D002", "FOK는 현재 지원하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_LIMIT_BID_INPUT("D003", "LIMIT-BID 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_LIMIT_ASK_INPUT("D004", "LIMIT-ASK 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_MARKET_BID_INPUT("D005", "MARKET-BID 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_MARKET_ASK_INPUT("D006", "MARKET-ASK 주문 입력이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),

    //user
    USER_NAME_REQUIRED("A001", "사용자 이름은 필수입니다.", HttpStatus.BAD_REQUEST);


    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
