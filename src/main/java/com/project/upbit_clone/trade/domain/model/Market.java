package com.project.upbit_clone.trade.domain.model;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.model.BaseEntity;
import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.domain.vo.NonNegativeAmount;
import com.project.upbit_clone.global.domain.vo.PositiveAmount;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Table(name = "market")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Market extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "market_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_asset_id", nullable = false)
    private Asset baseAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_asset_id", nullable = false)
    private Asset quoteAsset;

    @Column(name = "market_code", nullable = false, length = 20)
    private String marketCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EnumStatus status;

    @Column(name = "min_order_quote", precision = 30, scale = 8, nullable = false)
    private BigDecimal minOrderQuote;

    @Column(name = "tick_size", precision = 30, scale = 8, nullable = false)
    private BigDecimal tickSize;

    public static Market create(CreateCommand command) {
        validateCreateCommand(command);

        // Asset 검증
        if (isSameAsset(command.baseAsset(), command.quoteAsset())) {
            throw new BusinessException(ErrorCode.DIFFERENT_ASSET_REQUIRED);
        }
        return new Market(command);
    }

    // 동일한 자산인지 검증. (baseAsset과 quoteAsset이 같을순 없음)
    private static boolean isSameAsset(Asset baseAsset, Asset quoteAsset) {
        if (baseAsset == null || quoteAsset == null) {
            return false;
        }

        if (baseAsset.getId() != null && quoteAsset.getId() != null) {
            return baseAsset.getId().equals(quoteAsset.getId());
        }
        return baseAsset == quoteAsset;
    }

    private Market(CreateCommand command) {
        this.baseAsset = command.baseAsset();
        this.quoteAsset = command.quoteAsset();
        this.marketCode = command.marketCode();
        this.status = (command.status() == null) ? EnumStatus.ACTIVE : command.status();
        this.minOrderQuote = new NonNegativeAmount(command.minOrderQuote()).value();
        this.tickSize = new PositiveAmount(command.tickSize()).value();
    }

    public record CreateCommand(
            Asset baseAsset,
            Asset quoteAsset,
            String marketCode,
            EnumStatus status,
            BigDecimal minOrderQuote,
            BigDecimal tickSize
    ) {
    }

    // null 검증
    public static void validateCreateCommand(CreateCommand command) {
        if (command == null
                || command.baseAsset() == null
                || command.quoteAsset() == null
                || command.marketCode() == null
                || command.marketCode().isBlank()
                || command.minOrderQuote() == null
                || command.tickSize() == null) {
            throw new BusinessException(ErrorCode.INVALID_MARKET_INPUT);
        }
    }
}
