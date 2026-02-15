package com.project.upbit_clone.global.exception;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final transient Object data;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        this(errorCode, customMessage, null);
    }

    public BusinessException(ErrorCode errorCode, Object data) {
        this(errorCode, null, data);
    }

    public BusinessException(ErrorCode errorCode, String customMessage, Object data) {
        super(customMessage != null ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
        this.data = data;
    }
}
