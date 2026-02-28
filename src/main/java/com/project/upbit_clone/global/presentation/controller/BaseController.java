package com.project.upbit_clone.global.presentation.controller;

import com.project.upbit_clone.global.presentation.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    protected ResponseEntity<ApiResponse<Void>> ok() {
        return ResponseEntity.ok(ApiResponse.success());
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    protected ResponseEntity<ApiResponse<Void>> created() {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }
}
