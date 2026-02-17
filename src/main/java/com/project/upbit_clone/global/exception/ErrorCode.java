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
    QUOTE_WALLET_NOT_FOUND("G002", "Quote 지갑을 찾을 수 없습니다." , HttpStatus.NOT_FOUND),

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
    MISSING_REQUIRED_VALUE("D007","주문에 필요한 필수값이 누락되었습니다.", HttpStatus.BAD_REQUEST),

    //market
    DIFFERENT_ASSET_REQUIRED("C001", "서로 다른 자산이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_MARKET_INPUT("C002", "유효하지 않은 시장 입력입니다.", HttpStatus.BAD_REQUEST),
    MARKET_NOT_FOUND("C003", "일치하는 시장을 찾을 수 없습니다." , HttpStatus.NOT_FOUND),
    NOT_ALLOWED_MARKET_STATUS("C004", "유효하지 않은 시장 상태입니다.", HttpStatus.BAD_REQUEST),

    //trade
    SELF_TRADE_NOT_ALLOWED("B001", "판매자와 구매자는 같을 수 없습니다.", HttpStatus.BAD_REQUEST),
    TRADE_MARKET_NOT_MATCHED("B002", "거래 마켓과 주문 마켓은 같아야 합니다.", HttpStatus.BAD_REQUEST),
    TRADE_PRICE_MUST_BE_MAKER_PRICE("B003", "체결 가격은 메이커 주문 가격이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_TRADE_INPUT("B004", "유효하지 않은 거래 마켓 입력입니다.", HttpStatus.BAD_REQUEST),
    ORDER_SIDE_NOT_MATCHED("B005", "거래 방향이 일치해야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_SIDE("B006", "유효하지 않은 거래 방향입니다", HttpStatus.BAD_REQUEST),


    //user
    USER_NAME_REQUIRED("A001", "사용자 이름은 필수입니다.", HttpStatus.BAD_REQUEST),
    INVALID_USER_INPUT("A002", "유효하지 않은 사용자 입력입니다.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("A003", "일치하는 사용자를 찾을 수 없습니다." , HttpStatus.NOT_FOUND),
    NOT_ALLOWED_USER_STATUS("A004", "유효하지 않은 사용자 상태입니다." , HttpStatus.BAD_REQUEST);


    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
