package com.project.upbit_clone.global.domain.vo;

import java.math.BigDecimal;

public record PositiveAmount(BigDecimal value) {
    public PositiveAmount {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
    }
}
