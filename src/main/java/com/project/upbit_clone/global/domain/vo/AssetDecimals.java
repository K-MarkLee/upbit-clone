package com.project.upbit_clone.global.domain.vo;

public record AssetDecimals(Byte value) {
    public AssetDecimals {
        // TODO: 저장은 decimal 8이고 정책은 18이라 정리필요.
        if (value == null || value < 0 || value > 18) {
            throw new IllegalArgumentException("자산 소수점 자릿수는 0 이상 18 이하여야 합니다.");
        }
    }
}
