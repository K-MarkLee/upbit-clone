package com.project.upbit_clone.wallet.domain.model;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.wallet.domain.vo.LedgerType;
import com.project.upbit_clone.wallet.domain.vo.ChangeType;
import com.project.upbit_clone.wallet.domain.vo.ReferenceType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @Column(name = "type", nullable = false)
    private LedgerType type;

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


}
