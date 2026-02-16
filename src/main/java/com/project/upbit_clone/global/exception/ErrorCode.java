package com.project.upbit_clone.global.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public enum ErrorCode {
    //wallet
    INVALID_WALLET_INPUT("G001", "유효하지 않은 지갑 입력입니다.", HttpStatus.BAD_REQUEST),

    //asset
    INVALID_ASSET_INPUT("F001", "유효하지 않은 자산 입력입니다.", HttpStatus.BAD_REQUEST),

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

    //market
    DIFFERENT_ASSET_REQUIRED("C001", "서로 다른 자산이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_MARKET_INPUT("C002", "유효하지 않은 시장 입력입니다.", HttpStatus.BAD_REQUEST),

    //trade
    SELF_TRADE_NOT_ALLOWED("B001", "판매자와 구매자는 같을 수 없습니다.", HttpStatus.BAD_REQUEST),
    TRADE_MARKET_MISMATCH("B002", "거래 마켓과 주문 마켓은 같아야 합니다.", HttpStatus.BAD_REQUEST),
    TRADE_PRICE_MUST_BE_MAKER_PRICE("B003", "체결 가격은 메이커 주문 가격이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_TRADE_INPUT("B004", "유효하지 않은 거래 마켓 입력입니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_SIDE("B005", "거래 타입이 일치해야 합니다.", HttpStatus.BAD_REQUEST),


    //user
    USER_NAME_REQUIRED("A001", "사용자 이름은 필수입니다.", HttpStatus.BAD_REQUEST),
    INVALID_USER_INPUT("A002", "유효하지 않은 사용자 입력입니다.", HttpStatus.BAD_REQUEST);


    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
