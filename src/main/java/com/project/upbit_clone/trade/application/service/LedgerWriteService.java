package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.model.Trade;
import com.project.upbit_clone.wallet.domain.model.Ledger;
import com.project.upbit_clone.wallet.domain.model.Wallet;
import com.project.upbit_clone.wallet.domain.repository.LedgerRepository;
import com.project.upbit_clone.wallet.domain.vo.ChangeType;
import com.project.upbit_clone.wallet.domain.vo.LedgerType;
import com.project.upbit_clone.wallet.domain.vo.ReferenceType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

@Service
public class LedgerWriteService {

    private final LedgerRepository ledgerRepository;

    public LedgerWriteService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public BalanceSnapshot capture(Wallet wallet) {
        Objects.requireNonNull(wallet, "wallet은 null일 수 없습니다.");
        return new BalanceSnapshot(wallet.getAvailableBalance(), wallet.getLockedBalance());
    }

    public Ledger createOrderLedger(OrderLedgerSpec spec) {
        Objects.requireNonNull(spec, "spec은 null일 수 없습니다.");
        Objects.requireNonNull(spec.order(), "order는 null일 수 없습니다.");
        return createLedger(
                spec.wallet(),
                spec.before(),
                spec.after(),
                spec.mutation(),
                new LedgerReference(ReferenceType.ORDER, spec.order().getId()),
                spec.meta()
        );
    }

    public Ledger createTradeLedger(TradeLedgerSpec spec) {
        Objects.requireNonNull(spec, "spec은 null일 수 없습니다.");
        Objects.requireNonNull(spec.trade(), "trade는 null일 수 없습니다.");
        return createLedger(
                spec.wallet(),
                spec.before(),
                spec.after(),
                spec.mutation(),
                new LedgerReference(ReferenceType.TRADE, spec.trade().getId()),
                spec.meta()
        );
    }

    public Ledger save(Ledger ledger) {
        Objects.requireNonNull(ledger, "ledger는 null일 수 없습니다.");
        return ledgerRepository.save(ledger);
    }

    public void saveAll(Collection<Ledger> ledgers) {
        if (ledgers == null || ledgers.isEmpty()) {
            return;
        }
        ledgerRepository.saveAll(ledgers);
    }

    private Ledger createLedger(
            Wallet wallet,
            BalanceSnapshot before,
            BalanceSnapshot after,
            LedgerMutation mutation,
            LedgerReference reference,
            LedgerMeta meta
    ) {
        Objects.requireNonNull(wallet, "wallet은 null일 수 없습니다.");
        Objects.requireNonNull(before, "before는 null일 수 없습니다.");
        Objects.requireNonNull(after, "after는 null일 수 없습니다.");
        Objects.requireNonNull(mutation, "mutation은 null일 수 없습니다.");
        Objects.requireNonNull(reference, "reference는 null일 수 없습니다.");
        Objects.requireNonNull(meta, "meta는 null일 수 없습니다.");

        return Ledger.create(new Ledger.CreateCommand(
                wallet,
                wallet.getAsset(),
                mutation.ledgerType(),
                mutation.changeType(),
                mutation.amount(),
                before.availableBalance(),
                after.availableBalance(),
                before.lockedBalance(),
                after.lockedBalance(),
                reference.referenceType(),
                reference.referenceId(),
                meta.description(),
                meta.idempotencyKey()
        ));
    }

    public record BalanceSnapshot(
            BigDecimal availableBalance,
            BigDecimal lockedBalance
    ) {
    }

    public record LedgerMutation(
            LedgerType ledgerType,
            ChangeType changeType,
            BigDecimal amount
    ) {
    }

    public record LedgerMeta(
            String description,
            String idempotencyKey
    ) {
    }

    public record OrderLedgerSpec(
            Wallet wallet,
            BalanceSnapshot before,
            BalanceSnapshot after,
            LedgerMutation mutation,
            Order order,
            LedgerMeta meta
    ) {
    }

    public record TradeLedgerSpec(
            Wallet wallet,
            BalanceSnapshot before,
            BalanceSnapshot after,
            LedgerMutation mutation,
            Trade trade,
            LedgerMeta meta
    ) {
    }

    private record LedgerReference(
            ReferenceType referenceType,
            Long referenceId
    ) {
    }
}
