package com.project.upbit_clone.wallet.domain.model;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.model.BaseEntity;
import com.project.upbit_clone.global.domain.vo.NonNegativeAmount;
import com.project.upbit_clone.global.domain.vo.PositiveAmount;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.user.domain.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "wallet",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_user_asset", columnNames = {"user_id", "asset_id"})
        }
)
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
    private BigDecimal availableBalance;

    @Column(name = "locked_balance", precision = 30, scale = 8, nullable = false)
    private BigDecimal lockedBalance;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static Wallet create(User user, Asset asset, BigDecimal availableBalance, BigDecimal lockedBalance) {
        validateCreateInput(user, asset);

        return new Wallet(user, asset, availableBalance, lockedBalance);
    }

    private Wallet(User user, Asset asset, BigDecimal availableBalance, BigDecimal lockedBalance) {
        this.user = user;
        this.asset = asset;
        this.availableBalance = new NonNegativeAmount(availableBalance == null ? BigDecimal.ZERO : availableBalance).value();
        this.lockedBalance = new NonNegativeAmount(lockedBalance == null ? BigDecimal.ZERO : lockedBalance).value();
    }

    // null 검증.
    public static void validateCreateInput(User user, Asset asset) {
        if (user == null || asset == null) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_INPUT);
        }
    }

    // 사용 가능한 잔고를 잠금 잔고로 이동시킨다.
    public void lock(BigDecimal amount) {
        BigDecimal lockAmount = new PositiveAmount(amount).value();

        if (this.availableBalance.compareTo(lockAmount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_AVAILABLE_BALANCE);
        }

        this.availableBalance = this.availableBalance.subtract(lockAmount);
        this.lockedBalance = this.lockedBalance.add(lockAmount);
    }

    // 잠금 잔고를 사용 가능한 잔고로 이동시킨다.
    public void unlock(BigDecimal amount) {
        BigDecimal unlockAmount = new PositiveAmount(amount).value();

        if (this.lockedBalance.compareTo(unlockAmount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_LOCKED_BALANCE);
        }

        this.availableBalance = this.availableBalance.add(unlockAmount);
        this.lockedBalance = this.lockedBalance.subtract(unlockAmount);
    }

    public void increaseAvailable(BigDecimal amount) {
        BigDecimal value = new PositiveAmount(amount).value();
        this.availableBalance = this.availableBalance.add(value);
    }

    // 잠금 잔고만 감소시킨다. (체결 정산용)
    public void decreaseLocked(BigDecimal amount) {
        BigDecimal value = new PositiveAmount(amount).value();

        if (this.lockedBalance.compareTo(value) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_LOCKED_BALANCE);
        }

        this.lockedBalance = this.lockedBalance.subtract(value);
    }

}
