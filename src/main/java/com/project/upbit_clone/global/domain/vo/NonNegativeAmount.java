package com.project.upbit_clone.global.domain.vo;

import java.math.BigDecimal;

public record NonNegativeAmount(BigDecimal value) {
    public NonNegativeAmount {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다.");
        }
    }
}
