package com.project.upbit_clone.asset.domain.model;

import com.project.upbit_clone.global.domain.model.BaseEntity;
import com.project.upbit_clone.global.domain.vo.AssetDecimals;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;

import com.project.upbit_clone.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;

import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "asset")
public class Asset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long id;

    @Column(name = "symbol", nullable = false, length = 10)
    private String symbol;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "decimals", nullable = false)
    private Byte decimals;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EnumStatus status;

    public static Asset create(String symbol, String name, Byte decimals, EnumStatus status) {
        validateCreateInput(symbol, name, decimals);

        return new Asset(symbol, name, decimals, status);
    }

    private Asset(String symbol, String name, Byte decimals, EnumStatus status) {
        this.symbol = symbol;
        this.name = name;
        this.decimals = new AssetDecimals(decimals).value();
        this.status = (status == null) ? EnumStatus.ACTIVE : status;
    }

    // null 검증
    private static void validateCreateInput(String symbol, String name, Byte decimals) {
        if (symbol == null || name == null || decimals == null || symbol.isBlank() || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ASSET_INPUT);
        }
    }
}
