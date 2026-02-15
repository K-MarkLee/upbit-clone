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

    // user
    USER_NAME_REQUIRED("A001", "사용자 이름은 필수입니다.", HttpStatus.BAD_REQUEST);


    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}