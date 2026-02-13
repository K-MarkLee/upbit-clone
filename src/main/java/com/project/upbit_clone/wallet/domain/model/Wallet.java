package com.project.upbit_clone.wallet.domain.model;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.model.BaseEntity;
import com.project.upbit_clone.user.domain.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "wallet")
public class Wallet extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "available_balance", precision = 30, scale = 8, nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "locked_balance", precision = 30, scale = 8, nullable = false)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
