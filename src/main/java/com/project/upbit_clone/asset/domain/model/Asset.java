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

import java.util.Objects;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "asset",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_asset_symbol", columnNames = "symbol")
        }
)
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

    // 동일 자산인지 비교한다. (id가 없으면 symbol 기준으로 비교)
    public boolean isSameAsset(Asset other) {
        if (other == null) {
            return false;
        }
        if (this.id != null && other.id != null) {
            return Objects.equals(this.id, other.id);
        }
        return Objects.equals(this.symbol, other.symbol);
    }
}
