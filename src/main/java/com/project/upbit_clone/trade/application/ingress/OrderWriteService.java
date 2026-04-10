package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.application.service.LedgerWriteService;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.wallet.domain.model.Ledger;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.wallet.domain.model.Wallet;
import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import com.project.upbit_clone.wallet.domain.vo.ChangeType;
import com.project.upbit_clone.wallet.domain.vo.LedgerType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class OrderWriteService {

    private final CommandLogRepository commandLogRepository;
    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final LedgerWriteService ledgerWriteService;

    public OrderWriteService(
            CommandLogRepository commandLogRepository,
            OrderRepository orderRepository,
            WalletRepository walletRepository,
            LedgerWriteService ledgerWriteService
    ) {
        this.commandLogRepository = commandLogRepository;
        this.orderRepository = orderRepository;
        this.walletRepository = walletRepository;
        this.ledgerWriteService = ledgerWriteService;
    }

    @Transactional
    public AcceptedPlaceWrite writeAcceptedPlace(AcceptedPlaceCommand command) {
        Objects.requireNonNull(command, "command는 null일 수 없습니다.");
        Objects.requireNonNull(command.commandLog(), "commandLog는 null일 수 없습니다.");
        Objects.requireNonNull(command.user(), "user는 null일 수 없습니다.");
        Objects.requireNonNull(command.market(), "market은 null일 수 없습니다.");

        Order pendingOrder = Order.create(new Order.CreateCommand(
                command.market(),
                command.user(),
                command.clientOrderId(),
                command.orderKey(),
                command.orderSide(),
                command.orderType(),
                command.timeInForce(),
                command.price(),
                command.quantity(),
                command.quoteAmount()
        ));

        CommandLog savedCommandLog = commandLogRepository.saveAndFlush(command.commandLog());
        ReservedWallet reservedWallet = reserveWallet(command);
        Wallet savedWallet = walletRepository.saveAndFlush(reservedWallet.wallet());
        Order savedOrder = orderRepository.saveAndFlush(pendingOrder);
        Ledger savedLedger = ledgerWriteService.save(ledgerWriteService.createOrderLedger(
                new LedgerWriteService.OrderLedgerSpec(
                        savedWallet,
                        reservedWallet.beforeSnapshot(),
                        reservedWallet.afterSnapshot(),
                        new LedgerWriteService.LedgerMutation(
                                LedgerType.ORDER_LOCK,
                                ChangeType.DECREASE,
                                reservedWallet.lockAmount()
                        ),
                        savedOrder,
                        new LedgerWriteService.LedgerMeta(
                                "ORDER_LOCK",
                                orderLockIdempotencyKey(savedOrder)
                        )
                )
        ));

        return new AcceptedPlaceWrite(savedCommandLog, savedWallet, savedOrder, savedLedger);
    }

    private ReservedWallet reserveWallet(AcceptedPlaceCommand command) {
        Wallet wallet = walletRepository.findByUserIdAndAssetId(
                        command.user().getId(),
                        resolveLockAssetId(command)
                )
                .orElseThrow(() -> new BusinessException(resolveWalletNotFoundError(command.orderSide())));

        BigDecimal lockAmount = calculateLockAmount(command);
        LedgerWriteService.BalanceSnapshot beforeSnapshot = ledgerWriteService.capture(wallet);
        wallet.lock(lockAmount);
        LedgerWriteService.BalanceSnapshot afterSnapshot = ledgerWriteService.capture(wallet);
        return new ReservedWallet(wallet, beforeSnapshot, afterSnapshot, lockAmount);
    }

    private Long resolveLockAssetId(AcceptedPlaceCommand command) {
        return switch (command.orderSide()) {
            case BID -> command.market().getQuoteAsset().getId();
            case ASK -> command.market().getBaseAsset().getId();
        };
    }

    private ErrorCode resolveWalletNotFoundError(OrderSide orderSide) {
        return orderSide == OrderSide.BID
                ? ErrorCode.QUOTE_WALLET_NOT_FOUND
                : ErrorCode.BASE_WALLET_NOT_FOUND;
    }

    private BigDecimal calculateLockAmount(AcceptedPlaceCommand command) {
        return switch (command.orderSide()) {
            case BID -> resolveBidLockAmount(command);
            case ASK -> command.quantity();
        };
    }

    private BigDecimal resolveBidLockAmount(AcceptedPlaceCommand command) {
        if (command.orderType() == OrderType.MARKET) {
            return command.quoteAmount();
        }
        return command.price().multiply(command.quantity());
    }

    private String orderLockIdempotencyKey(Order order) {
        return "order:" + order.getOrderKey() + ":lock";
    }

    public record AcceptedPlaceCommand(
            CommandLog commandLog,
            User user,
            Market market,
            String clientOrderId,
            String orderKey,
            OrderSide orderSide,
            OrderType orderType,
            TimeInForce timeInForce,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount
    ) {
    }

    public record AcceptedPlaceWrite(
            CommandLog commandLog,
            Wallet wallet,
            Order order,
            Ledger ledger
    ) {
    }

    private record ReservedWallet(
            Wallet wallet,
            LedgerWriteService.BalanceSnapshot beforeSnapshot,
            LedgerWriteService.BalanceSnapshot afterSnapshot,
            BigDecimal lockAmount
    ) {
    }
}
