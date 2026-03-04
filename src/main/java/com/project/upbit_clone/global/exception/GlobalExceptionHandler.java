package com.project.upbit_clone.global.exception;

import com.project.upbit_clone.global.presentation.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<Object> body = ApiResponse.failure(
                errorCode.getCode(),
                exception.getMessage(),
                exception.getData()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        return handleBindingResultValidation(exception.getBindingResult());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException exception) {
        return handleBindingResultValidation(exception.getBindingResult());
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> handleBindingResultValidation(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return badRequestValidation(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String path = violation.getPropertyPath() != null
                    ? violation.getPropertyPath().toString()
                    : "unknown";
            errors.put(path, violation.getMessage());
        }

        return badRequestValidation(errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER_TYPE;
        String message = "'" + exception.getName() + "' " + errorCode.getMessage();
        ApiResponse<Void> body = ApiResponse.failure(errorCode.getCode(), message);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception
    ) {
        String message = "'" + exception.getParameterName() + "' 요청 파라미터가 필요합니다.";
        return frameworkError(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        return frameworkError(HttpStatus.BAD_REQUEST, "요청 본문(JSON) 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException exception) {
        return frameworkError(exception.getStatusCode(), exception.getReason());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        return frameworkError(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            errorResponse.getBody();
            String detail = errorResponse.getBody().getDetail();
            return frameworkError(errorResponse.getStatusCode(), detail);
        }

        log.error("Unhandled exception", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ApiResponse<Void> body = ApiResponse.failure(errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> badRequestValidation(Map<String, String> errors) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        ApiResponse<Map<String, String>> body = ApiResponse.failure(
                errorCode.getCode(),
                errorCode.getMessage(),
                errors
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    private ResponseEntity<ApiResponse<Void>> frameworkError(HttpStatusCode statusCode, String message) {
        ErrorCode errorCode = statusCode.is4xxClientError()
                ? ErrorCode.VALIDATION_ERROR
                : ErrorCode.INTERNAL_SERVER_ERROR;
        String resolvedMessage = (message == null || message.isBlank())
                ? errorCode.getMessage()
                : message;
        ApiResponse<Void> body = ApiResponse.failure(errorCode.getCode(), resolvedMessage);
        return ResponseEntity.status(statusCode).body(body);
    }
}
