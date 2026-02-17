package com.project.upbit_clone.wallet.domain.model;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.domain.vo.NonNegativeAmount;
import com.project.upbit_clone.global.domain.vo.PositiveAmount;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.wallet.domain.vo.LedgerType;
import com.project.upbit_clone.wallet.domain.vo.ChangeType;
import com.project.upbit_clone.wallet.domain.vo.ReferenceType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ledger")
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "ledger_type", nullable = false)
    private LedgerType ledgerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    @Column(name = "amount", precision = 30, scale = 8, nullable = false)
    private BigDecimal amount;

    @Column(name = "available_before", precision = 30, scale = 8, nullable = false)
    private BigDecimal availableBefore;

    @Column(name = "available_after", precision = 30, scale = 8, nullable = false)
    private BigDecimal availableAfter;

    @Column(name = "locked_before", precision = 30, scale = 8, nullable = false)
    private BigDecimal lockedBefore;

    @Column(name = "locked_after", precision = 30, scale = 8, nullable = false)
    private BigDecimal lockedAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private ReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "description")
    private String description;

    @Column(name = "idempotency_key", nullable = false, length = 150, unique = true)
    private String idempotencyKey;

    // TODO: timestamp(3) ddl적용 예정
    @Column(name="created_at", insertable=false, updatable=false, nullable=false)
    private LocalDateTime createdAt;

    public static Ledger create(CreateCommand command) {
        validateCreateCommand(command);

        validateWalletAssetMatch(command);

        return new Ledger(command);
    }

    private Ledger(CreateCommand command) {
        this.wallet = command.wallet();
        this.asset = command.asset();
        this.ledgerType = command.ledgerType();
        this.changeType = command.changeType();
        this.amount = new PositiveAmount(command.amount()).value();
        this.availableBefore = new NonNegativeAmount(command.availableBefore()).value();
        this.availableAfter = new NonNegativeAmount(command.availableAfter()).value();
        this.lockedBefore = new NonNegativeAmount(command.lockedBefore()).value();
        this.lockedAfter = new NonNegativeAmount(command.lockedAfter()).value();
        this.referenceType = command.referenceType();
        this.referenceId = command.referenceId();
        this.description = command.description();
        this.idempotencyKey = command.idempotencyKey();

    }

    public record CreateCommand(
            Wallet wallet,
            Asset asset,
            LedgerType ledgerType,
            ChangeType changeType,
            BigDecimal amount,
            BigDecimal availableBefore,
            BigDecimal availableAfter,
            BigDecimal lockedBefore,
            BigDecimal lockedAfter,
            ReferenceType referenceType,
            Long referenceId,
            String description,
            String idempotencyKey
    ) {
    }

    // null 검증.
    public static void validateCreateCommand(CreateCommand command) {
        if (command == null
                || command.wallet() == null
                || command.asset() == null
                || command.ledgerType() == null
                || command.changeType() == null
                || command.amount() == null
                || command.availableBefore() == null
                || command.availableAfter() == null
                || command.lockedBefore() == null
                || command.lockedAfter() == null
                || command.referenceType() == null
                || command.idempotencyKey() == null
                || command.idempotencyKey().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_LEDGER_INPUT);
        }
    }

    // wallet의 자산과 ledger의 자산이 동일한지 검증.
    private static void validateWalletAssetMatch(CreateCommand command) {
        if (!Objects.equals(command.wallet().getAsset().getId(), command.asset().getId())) {
            throw new BusinessException(ErrorCode.ASSET_NOT_MATCHED);
        }
    }

}
