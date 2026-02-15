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

    // TODO : 디폴트 값 'ACTIVE' 필요
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EnumStatus status;

    @Column(name = "min_order_quote", precision = 30, scale = 8, nullable = false)
    private BigDecimal minOrderQuote;

    @Column(name = "tick_size", precision = 30, scale = 8, nullable = false)
    private BigDecimal tickSize;

    public static Market create(CreateCommand command) {
        if (command.baseAsset().getId().equals(command.quoteAsset().getId())) {
            throw new BusinessException(ErrorCode.DIFFERENT_ASSET_REQUIRED);
        }
        return new Market(command);
    }

    private Market(CreateCommand command) {
        this.baseAsset = command.baseAsset();
        this.quoteAsset = command.quoteAsset();
        this.marketCode = command.marketCode();
        this.status = EnumStatus.ACTIVE;
        this.minOrderQuote = new NonNegativeAmount(command.minOrderQuote()).value();
        this.tickSize = new PositiveAmount(command.tickSize()).value();
    }

    public record CreateCommand(
            Asset baseAsset,
            Asset quoteAsset,
            String marketCode,
            BigDecimal minOrderQuote,
            BigDecimal tickSize
    ) {
    }
}
