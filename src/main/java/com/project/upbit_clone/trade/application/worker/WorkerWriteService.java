package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.asset.domain.model.Asset;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.EngineResult;
import com.project.upbit_clone.trade.application.engine.orderbook.InMemoryOrderBook;
import com.project.upbit_clone.trade.application.service.LedgerWriteService;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.model.Trade;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.repository.TradeRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.infrastructure.persistence.model.CommandLog;
import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffset;
import com.project.upbit_clone.trade.infrastructure.persistence.model.ConsumerOffsetId;
import com.project.upbit_clone.trade.infrastructure.persistence.model.EventLog;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.CommandLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.ConsumerOffsetRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.repository.EventLogRepository;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.EventType;
import com.project.upbit_clone.trade.infrastructure.persistence.vo.LogType;
import com.project.upbit_clone.wallet.domain.model.Ledger;
import com.project.upbit_clone.wallet.domain.model.Wallet;
import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import com.project.upbit_clone.wallet.domain.vo.ChangeType;
import com.project.upbit_clone.wallet.domain.vo.LedgerType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class WorkerWriteService {
    // TODO : MVP용으로 repo참조하기로 함. 이후 수정.

    private static final String COMMAND_CONSUMER_NAME = "market-worker";
    private static final String ORDER_KEY_FIELD = "orderKey";

    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final TradeRepository tradeRepository;
    private final CommandLogRepository commandLogRepository;
    private final EventLogRepository eventLogRepository;
    private final ConsumerOffsetRepository consumerOffsetRepository;
    private final LedgerWriteService ledgerWriteService;
    private final JsonMapper jsonMapper;

    public WorkerWriteService(
            OrderRepository orderRepository,
            WalletRepository walletRepository,
            TradeRepository tradeRepository,
            CommandLogRepository commandLogRepository,
            EventLogRepository eventLogRepository,
            ConsumerOffsetRepository consumerOffsetRepository,
            LedgerWriteService ledgerWriteService,
            JsonMapper jsonMapper
    ) {
        this.orderRepository = orderRepository;
        this.walletRepository = walletRepository;
        this.tradeRepository = tradeRepository;
        this.commandLogRepository = commandLogRepository;
        this.eventLogRepository = eventLogRepository;
        this.consumerOffsetRepository = consumerOffsetRepository;
        this.ledgerWriteService = ledgerWriteService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void writePlace(CommandMessage.Place message, EngineResult.PlaceResult result) {
        Objects.requireNonNull(message, "message는 null값일 수 없습니다.");
        Objects.requireNonNull(result, "result는 null값일 수 없습니다.");

        // db write에 필요한 정보 우선 db - read.
        Order taker = orderRepository.findByOrderKey(message.orderKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Map<String, Order> makerOrdersByKey = loadMakerOrders(result.fills());
        Map<WalletKey, Wallet> walletsByKey = loadWallets(taker, makerOrdersByKey.values());
        List<PendingTradeLedgerWrite> pendingTradeLedgerWrites = new ArrayList<>();
        List<PendingOrderLedgerWrite> pendingOrderLedgerWrites = new ArrayList<>();
        List<Trade> trades = applyFills(
                taker,
                result,
                makerOrdersByKey,
                walletsByKey,
                pendingTradeLedgerWrites
        );

        finalizeTakerState(taker, result, walletsByKey, pendingOrderLedgerWrites, message.commandLogId());

        orderRepository.saveAll(uniqueOrders(taker, makerOrdersByKey.values()));
        walletRepository.saveAllAndFlush(walletsByKey.values());
        if (!trades.isEmpty()) {
            tradeRepository.saveAllAndFlush(trades);
        }
        ledgerWriteService.saveAll(createTradeLedgers(pendingTradeLedgerWrites, pendingOrderLedgerWrites));

        CommandLog commandLogRef = commandLogRepository.getReferenceById(message.commandLogId());
        eventLogRepository.saveAll(createEventLogs(commandLogRef, taker, message, result, trades));
        consumerOffsetRepository.save(createConsumerOffset(message.marketId(), message.commandLogId()));
    }

    @Transactional
    public void writeCancel(CommandMessage.Cancel message, InMemoryOrderBook.LevelDelta removedLevelDelta) {
        Objects.requireNonNull(message, "message는 null값일 수 없습니다.");

        Order targetOrder = orderRepository.findByOrderKey(message.targetOrderKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (targetOrder.getStatus() != OrderStatus.PENDING && targetOrder.getStatus() != OrderStatus.OPEN) {
            throw new BusinessException(ErrorCode.ORDER_NOT_OPEN);
        }

        Map<WalletKey, Wallet> walletsByKey = loadWallets(targetOrder, List.of());
        Wallet sourceWallet = requireSourceWallet(walletsByKey, targetOrder);
        BigDecimal unlockAmount = remainingLockedAmount(targetOrder);
        String cancelReason = EngineResult.CancelReason.USER_REQUEST.name();
        List<PendingOrderLedgerWrite> pendingOrderLedgerWrites = new ArrayList<>();

        targetOrder.cancel(cancelReason);
        if (unlockAmount.compareTo(BigDecimal.ZERO) > 0) {
            LedgerWriteService.BalanceSnapshot beforeUnlock = ledgerWriteService.capture(sourceWallet);
            sourceWallet.unlock(unlockAmount);
            LedgerWriteService.BalanceSnapshot afterUnlock = ledgerWriteService.capture(sourceWallet);
            pendingOrderLedgerWrites.add(new PendingOrderLedgerWrite(
                    sourceWallet,
                    beforeUnlock,
                    afterUnlock,
                    LedgerType.ORDER_UNLOCK,
                    ChangeType.INCREASE,
                    unlockAmount,
                    targetOrder,
                    cancelReason,
                    orderUnlockIdempotencyKey(targetOrder, message.commandLogId())
            ));
        }

        orderRepository.save(targetOrder);
        walletRepository.saveAllAndFlush(walletsByKey.values());
        ledgerWriteService.saveAll(createOrderLedgers(pendingOrderLedgerWrites));

        CommandLog commandLogRef = commandLogRepository.getReferenceById(message.commandLogId());
        eventLogRepository.saveAll(createCancelEventLogs(
                commandLogRef,
                targetOrder,
                message,
                cancelReason,
                unlockAmount,
                removedLevelDelta
        ));
        consumerOffsetRepository.save(createConsumerOffset(message.marketId(), message.commandLogId()));
    }

    // order key로 maker 주문 한번에 조회.
    private Map<String, Order> loadMakerOrders(List<EngineResult.Fill> fills) {
        if (fills == null || fills.isEmpty()) {
            return Map.of();
        }

        List<String> makerOrderKeys = fills.stream()
                .map(EngineResult.Fill::makerOrderKey)
                .distinct()
                .toList();

        List<Order> makerOrders = orderRepository.findAllByOrderKeyIn(makerOrderKeys);
        if (makerOrders.size() != makerOrderKeys.size()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        Map<String, Order> makerOrdersByKey = new LinkedHashMap<>();
        for (Order makerOrder : makerOrders) {
            makerOrdersByKey.put(makerOrder.getOrderKey(), makerOrder);
        }
        return makerOrdersByKey;
    }

    // user id로 지갑 조회.
    private Map<WalletKey, Wallet> loadWallets(Order taker, Collection<Order> makerOrders) {
        Set<Long> userIds = new LinkedHashSet<>();
        userIds.add(taker.getUser().getId());
        for (Order makerOrder : makerOrders) {
            userIds.add(makerOrder.getUser().getId());
        }

        Set<Long> assetIds = new LinkedHashSet<>();
        assetIds.add(taker.getMarket().getBaseAsset().getId());
        assetIds.add(taker.getMarket().getQuoteAsset().getId());

        List<Wallet> wallets = walletRepository.findAllByUserIdInAndAssetIdIn(userIds, assetIds);

        Map<WalletKey, Wallet> walletsByKey = new LinkedHashMap<>();
        for (Wallet wallet : wallets) {
            walletsByKey.put(WalletKey.from(wallet), wallet);
        }
        return walletsByKey;
    }

    // fill 을 순차적으로 읽고 trade와 wallet 상태 반영.
    private List<Trade> applyFills(
            Order taker,
            EngineResult.PlaceResult result,
            Map<String, Order> makerOrdersByKey,
            Map<WalletKey, Wallet> walletsByKey,
            List<PendingTradeLedgerWrite> pendingTradeLedgerWrites
    ) {
        List<Trade> trades = new ArrayList<>();

        for (int index = 0; index < result.fills().size(); index++) {
            EngineResult.Fill fill = result.fills().get(index);
            Order maker = makerOrdersByKey.get(fill.makerOrderKey());
            if (maker == null) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }

            taker.applyExecutedQuantity(fill.executedQuantity(), fill.executedQuoteAmount());
            maker.applyExecutedQuantity(fill.executedQuantity(), fill.executedQuoteAmount());

            Trade trade = createTrade(taker, maker, fill, index);
            applyWalletSettlement(taker, maker, fill, trade, walletsByKey, pendingTradeLedgerWrites);
            trades.add(trade);
        }

        return trades;
    }

    // 지갑에서 주문 처리후 계산.
    private void applyWalletSettlement(
            Order taker,
            Order maker,
            EngineResult.Fill fill,
            Trade trade,
            Map<WalletKey, Wallet> walletsByKey,
            List<PendingTradeLedgerWrite> pendingTradeLedgerWrites
    ) {
        Wallet takerSourceWallet = requireSourceWallet(walletsByKey, taker);
        Wallet takerTargetWallet = getOrCreateTargetWallet(walletsByKey, taker);
        Wallet makerSourceWallet = requireSourceWallet(walletsByKey, maker);
        Wallet makerTargetWallet = getOrCreateTargetWallet(walletsByKey, maker);

        if (taker.getOrderSide() == OrderSide.BID) {
            WalletMutationSnapshots takerSourceSnapshots = applyWalletMutation(
                    takerSourceWallet,
                    wallet -> wallet.decreaseLocked(fill.executedQuoteAmount())
            );

            recordTradeLedgerWrite(
                    pendingTradeLedgerWrites,
                    takerSourceWallet,
                    takerSourceSnapshots.before(),
                    takerSourceSnapshots.after(),
                    ChangeType.DECREASE,
                    fill.executedQuoteAmount(),
                    trade,
                    "BUYER_QUOTE_LOCKED_DECREASE",
                    tradeLedgerIdempotencyKey(trade, "buyer-quote-locked")
            );

            WalletMutationSnapshots takerTargetSnapshots = applyWalletMutation(
                    takerTargetWallet,
                    wallet -> wallet.increaseAvailable(fill.executedQuantity())
            );

            recordTradeLedgerWrite(
                    pendingTradeLedgerWrites,
                    takerTargetWallet,
                    takerTargetSnapshots.before(),
                    takerTargetSnapshots.after(),
                    ChangeType.INCREASE,
                    fill.executedQuantity(),
                    trade,
                    "BUYER_BASE_AVAILABLE_INCREASE",
                    tradeLedgerIdempotencyKey(trade, "buyer-base-available")
            );

            WalletMutationSnapshots makerSourceSnapshots = applyWalletMutation(
                    makerSourceWallet,
                    wallet -> wallet.decreaseLocked(fill.executedQuantity())
            );

            recordTradeLedgerWrite(
                    pendingTradeLedgerWrites,
                    makerSourceWallet,
                    makerSourceSnapshots.before(),
                    makerSourceSnapshots.after(),
                    ChangeType.DECREASE,
                    fill.executedQuantity(),
                    trade,
                    "SELLER_BASE_LOCKED_DECREASE",
                    tradeLedgerIdempotencyKey(trade, "seller-base-locked")
            );

            WalletMutationSnapshots makerTargetSnapshots = applyWalletMutation(
                    makerTargetWallet,
                    wallet -> wallet.increaseAvailable(fill.executedQuoteAmount())
            );

            recordTradeLedgerWrite(
                    pendingTradeLedgerWrites,
                    makerTargetWallet,
                    makerTargetSnapshots.before(),
                    makerTargetSnapshots.after(),
                    ChangeType.INCREASE,
                    fill.executedQuoteAmount(),
                    trade,
                    "SELLER_QUOTE_AVAILABLE_INCREASE",
                    tradeLedgerIdempotencyKey(trade, "seller-quote-available")
            );
            return;
        }

        WalletMutationSnapshots takerSourceSnapshots = applyWalletMutation(
                takerSourceWallet,
                wallet -> wallet.decreaseLocked(fill.executedQuantity())
        );

        recordTradeLedgerWrite(
                pendingTradeLedgerWrites,
                takerSourceWallet,
                takerSourceSnapshots.before(),
                takerSourceSnapshots.after(),
                ChangeType.DECREASE,
                fill.executedQuantity(),
                trade,
                "SELLER_BASE_LOCKED_DECREASE",
                tradeLedgerIdempotencyKey(trade, "seller-base-locked")
        );

        WalletMutationSnapshots takerTargetSnapshots = applyWalletMutation(
                takerTargetWallet,
                wallet -> wallet.increaseAvailable(fill.executedQuoteAmount())
        );

        recordTradeLedgerWrite(
                pendingTradeLedgerWrites,
                takerTargetWallet,
                takerTargetSnapshots.before(),
                takerTargetSnapshots.after(),
                ChangeType.INCREASE,
                fill.executedQuoteAmount(),
                trade,
                "SELLER_QUOTE_AVAILABLE_INCREASE",
                tradeLedgerIdempotencyKey(trade, "seller-quote-available")
        );

        WalletMutationSnapshots makerSourceSnapshots = applyWalletMutation(
                makerSourceWallet,
                wallet -> wallet.decreaseLocked(fill.executedQuoteAmount())
        );

        recordTradeLedgerWrite(
                pendingTradeLedgerWrites,
                makerSourceWallet,
                makerSourceSnapshots.before(),
                makerSourceSnapshots.after(),
                ChangeType.DECREASE,
                fill.executedQuoteAmount(),
                trade,
                "BUYER_QUOTE_LOCKED_DECREASE",
                tradeLedgerIdempotencyKey(trade, "buyer-quote-locked")
        );

        WalletMutationSnapshots makerTargetSnapshots = applyWalletMutation(
                makerTargetWallet,
                wallet -> wallet.increaseAvailable(fill.executedQuantity())
        );

        recordTradeLedgerWrite(
                pendingTradeLedgerWrites,
                makerTargetWallet,
                makerTargetSnapshots.before(),
                makerTargetSnapshots.after(),
                ChangeType.INCREASE,
                fill.executedQuantity(),
                trade,
                "BUYER_BASE_AVAILABLE_INCREASE",
                tradeLedgerIdempotencyKey(trade, "buyer-base-available")
        );
    }

    private WalletMutationSnapshots applyWalletMutation(
            Wallet wallet,
            Consumer<Wallet> mutation
    ) {
        LedgerWriteService.BalanceSnapshot before = ledgerWriteService.capture(wallet);
        mutation.accept(wallet);
        LedgerWriteService.BalanceSnapshot after = ledgerWriteService.capture(wallet);
        return new WalletMutationSnapshots(before, after);
    }

    // 주문 상태 최종 수정.
    private void finalizeTakerState(
            Order taker,
            EngineResult.PlaceResult result,
            Map<WalletKey, Wallet> walletsByKey,
            List<PendingOrderLedgerWrite> pendingOrderLedgerWrites,
            Long commandLogId
    ) {
        Wallet takerSourceWallet = requireSourceWallet(walletsByKey, taker);

        switch (result.takerStatus()) {
            case OPEN -> {
                if (taker.getStatus() == OrderStatus.PENDING) {
                    taker.markOpen();
                }
            }
            case FILLED -> {
                // applyFills(...)가 FILLED까지 올린다.
            }
            case CANCELED -> taker.cancel(resolveCancelReason(result.cancelReason()));
            case PENDING -> throw new IllegalArgumentException("PENDING 상태는 worker 최종 상태가 될 수 없습니다.");
        }

        if (result.unlockAmount().compareTo(BigDecimal.ZERO) > 0) {
            LedgerWriteService.BalanceSnapshot beforeUnlock = ledgerWriteService.capture(takerSourceWallet);
            takerSourceWallet.unlock(result.unlockAmount());
            LedgerWriteService.BalanceSnapshot afterUnlock = ledgerWriteService.capture(takerSourceWallet);
            pendingOrderLedgerWrites.add(new PendingOrderLedgerWrite(
                    takerSourceWallet,
                    beforeUnlock,
                    afterUnlock,
                    LedgerType.ORDER_UNLOCK,
                    ChangeType.INCREASE,
                    result.unlockAmount(),
                    taker,
                    unlockReason(result),
                    orderUnlockIdempotencyKey(taker, commandLogId)
            ));
        }
    }

    // taker의 다중체결 주문 중복 제거.
    private List<Order> uniqueOrders(Order taker, Collection<Order> makerOrders) {
        Map<String, Order> ordersByKey = new LinkedHashMap<>();
        ordersByKey.put(taker.getOrderKey(), taker);
        for (Order makerOrder : makerOrders) {
            ordersByKey.put(makerOrder.getOrderKey(), makerOrder);
        }
        return new ArrayList<>(ordersByKey.values());
    }

    // trade 생성.
    private Trade createTrade(Order taker, Order maker, EngineResult.Fill fill, int index) {
        Order buyOrder = taker.getOrderSide() == OrderSide.BID ? taker : maker;
        Order sellOrder = taker.getOrderSide() == OrderSide.ASK ? taker : maker;

        return Trade.create(new Trade.CreateCommand(
                taker.getMarket(),
                buyOrder,
                sellOrder,
                tradeKey(taker, index),
                maker.getOrderSide(),
                fill.price(),
                fill.executedQuantity(),
                fill.executedQuoteAmount(),
                // TODO: fee rate 설정 필요
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ));
    }

    private List<Ledger> createTradeLedgers(
            List<PendingTradeLedgerWrite> pendingTradeLedgerWrites,
            List<PendingOrderLedgerWrite> pendingOrderLedgerWrites
    ) {
        List<Ledger> ledgers = new ArrayList<>();
        ledgers.addAll(createTradeLedgers(pendingTradeLedgerWrites));
        ledgers.addAll(createOrderLedgers(pendingOrderLedgerWrites));
        return ledgers;
    }

    private List<Ledger> createTradeLedgers(List<PendingTradeLedgerWrite> pendingTradeLedgerWrites) {
        List<Ledger> ledgers = new ArrayList<>();
        for (PendingTradeLedgerWrite pending : pendingTradeLedgerWrites) {
            ledgers.add(ledgerWriteService.createTradeLedger(
                    new LedgerWriteService.TradeLedgerSpec(
                            pending.wallet(),
                            pending.beforeSnapshot(),
                            pending.afterSnapshot(),
                            new LedgerWriteService.LedgerMutation(
                                    LedgerType.TRADE,
                                    pending.changeType(),
                                    pending.amount()
                            ),
                            pending.trade(),
                            new LedgerWriteService.LedgerMeta(
                                    pending.description(),
                                    pending.idempotencyKey()
                            )
                    )
            ));
        }
        return ledgers;
    }

    private List<Ledger> createOrderLedgers(List<PendingOrderLedgerWrite> pendingOrderLedgerWrites) {
        List<Ledger> ledgers = new ArrayList<>();
        for (PendingOrderLedgerWrite pending : pendingOrderLedgerWrites) {
            ledgers.add(ledgerWriteService.createOrderLedger(
                    new LedgerWriteService.OrderLedgerSpec(
                            pending.wallet(),
                            pending.beforeSnapshot(),
                            pending.afterSnapshot(),
                            new LedgerWriteService.LedgerMutation(
                                    pending.ledgerType(),
                                    pending.changeType(),
                                    pending.amount()
                            ),
                            pending.order(),
                            new LedgerWriteService.LedgerMeta(
                                    pending.description(),
                                    pending.idempotencyKey()
                            )
                    )
            ));
        }
        return ledgers;
    }

    private void recordTradeLedgerWrite(
            List<PendingTradeLedgerWrite> pendingTradeLedgerWrites,
            Wallet wallet,
            LedgerWriteService.BalanceSnapshot beforeSnapshot,
            LedgerWriteService.BalanceSnapshot afterSnapshot,
            ChangeType changeType,
            BigDecimal amount,
            Trade trade,
            String description,
            String idempotencyKey
    ) {
        pendingTradeLedgerWrites.add(new PendingTradeLedgerWrite(
                wallet,
                beforeSnapshot,
                afterSnapshot,
                changeType,
                amount,
                trade,
                description,
                idempotencyKey
        ));
    }

    // 주문에 대한 event log들 생성.
    private List<EventLog> createEventLogs(
            CommandLog commandLogRef,
            Order taker,
            CommandMessage.Place message,
            EngineResult.PlaceResult result,
            List<Trade> trades
    ) {
        List<EventLog> events = new ArrayList<>();
        int eventSequence = 1;

        for (Trade trade : trades) {
            events.add(createEventLog(
                    commandLogRef,
                    eventId(message.commandLogId(), eventSequence++),
                    EventType.TRADE_EXECUTED,
                    message.marketId(),
                    taker.getId(),
                    tradePayload(trade)
            ));
        }

        if (result.hasExecution() && result.takerStatus() != OrderStatus.FILLED) {
            events.add(createEventLog(
                    commandLogRef,
                    eventId(message.commandLogId(), eventSequence++),
                    EventType.ORDER_PARTIALLY_FILLED,
                    message.marketId(),
                    taker.getId(),
                    statusPayload(
                            taker,
                            result.takerStatus(),
                            result.executedQuantity(),
                            result.executedQuoteAmount(),
                            result.remainingQuantity(),
                            null
                    )
            ));
        }

        events.add(createEventLog(
                commandLogRef,
                eventId(message.commandLogId(), eventSequence++),
                resolveTerminalEventType(result.takerStatus()),
                message.marketId(),
                taker.getId(),
                statusPayload(taker, result)
        ));

        if (result.unlockAmount().compareTo(BigDecimal.ZERO) > 0) {
            events.add(createEventLog(
                    commandLogRef,
                    eventId(message.commandLogId(), eventSequence++),
                    EventType.FUNDS_UNLOCKED,
                    message.marketId(),
                    taker.getId(),
                    unlockedPayload(taker, result.unlockAmount(), "MATCH_REMAINDER_UNLOCK")
            ));
        }

        for (EngineResult.BookDelta bookDelta : result.bookDeltas()) {
            events.add(createEventLog(
                    commandLogRef,
                    eventId(message.commandLogId(), eventSequence++),
                    EventType.ORDER_BOOK_DELTA,
                    message.marketId(),
                    taker.getId(),
                    bookDeltaPayload(bookDelta)
            ));
        }

        return events;
    }

    private List<EventLog> createCancelEventLogs(
            CommandLog commandLogRef,
            Order targetOrder,
            CommandMessage.Cancel message,
            String cancelReason,
            BigDecimal unlockAmount,
            InMemoryOrderBook.LevelDelta removedLevelDelta
    ) {
        List<EventLog> events = new ArrayList<>();
        int eventSequence = 1;

        events.add(createEventLog(
                commandLogRef,
                eventId(message.commandLogId(), eventSequence++),
                EventType.ORDER_CANCELED,
                message.marketId(),
                targetOrder.getId(),
                statusPayload(
                    targetOrder,
                    OrderStatus.CANCELED,
                    targetOrder.getExecutedQuantity(),
                    targetOrder.getExecutedQuoteAmount(),
                    remainingQuantityForStatusPayload(targetOrder),
                    cancelReason
            )
        ));

        if (unlockAmount.compareTo(BigDecimal.ZERO) > 0) {
            events.add(createEventLog(
                    commandLogRef,
                    eventId(message.commandLogId(), eventSequence++),
                    EventType.FUNDS_UNLOCKED,
                    message.marketId(),
                    targetOrder.getId(),
                    unlockedPayload(targetOrder, unlockAmount, cancelReason)
            ));
        }

        if (removedLevelDelta != null) {
            events.add(createEventLog(
                    commandLogRef,
                    eventId(message.commandLogId(), eventSequence),
                    EventType.ORDER_BOOK_DELTA,
                    message.marketId(),
                    targetOrder.getId(),
                    bookDeltaPayload(new EngineResult.BookDelta(
                            removedLevelDelta,
                            EngineResult.BookDeltaReason.ORDER_CANCELED
                    ))
            ));
        }

        return events;
    }

    // event log 생성.
    private EventLog createEventLog(
            CommandLog commandLogRef,
            String eventId,
            EventType eventType,
            Long marketId,
            Long orderId,
            Map<String, Object> payload
    ) {
        return EventLog.create(new EventLog.CreateCommand(
                commandLogRef,
                eventId,
                eventType,
                marketId,
                orderId,
                toJson(payload)
        ));
    }

    // command offset 생성.
    private ConsumerOffset createConsumerOffset(Long marketId, Long commandLogId) {
        return ConsumerOffset.create(
                new ConsumerOffsetId(LogType.COMMAND, COMMAND_CONSUMER_NAME, String.valueOf(marketId)),
                commandLogId
        );
    }

    // 주문 상태에 따른 event type 변환.
    private EventType resolveTerminalEventType(OrderStatus status) {
        return switch (status) {
            case OPEN -> EventType.ORDER_OPENED;
            case FILLED -> EventType.ORDER_FILLED;
            case CANCELED -> EventType.ORDER_CANCELED;
            case PENDING -> throw new IllegalArgumentException("PENDING 상태는 최종 이벤트가 될 수 없습니다.");
        };
    }

    // payload map 생성.
    private Map<String, Object> statusPayload(Order taker, EngineResult.PlaceResult result) {
        return statusPayload(
                taker,
                result.takerStatus(),
                result.executedQuantity(),
                result.executedQuoteAmount(),
                result.remainingQuantity(),
                resolveCancelReason(result.cancelReason())
        );
    }

    private Map<String, Object> statusPayload(
            Order order,
            OrderStatus status,
            BigDecimal executedQuantity,
            BigDecimal executedQuoteAmount,
            BigDecimal remainingQuantity,
            String cancelReason
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(ORDER_KEY_FIELD, order.getOrderKey());
        payload.put("status", status.name());
        payload.put("executedQuantity", plain(executedQuantity));
        payload.put("executedQuoteAmount", plain(executedQuoteAmount));
        payload.put("remainingQuantity", plainNullable(remainingQuantity));
        if (cancelReason != null) {
            payload.put("cancelReason", cancelReason);
        }
        return payload;
    }

    private Map<String, Object> unlockedPayload(Order order, BigDecimal unlockAmount, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(ORDER_KEY_FIELD, order.getOrderKey());
        payload.put("userId", order.getUser().getId());
        payload.put("unlockAmount", plain(unlockAmount));
        payload.put("assetId", sourceAsset(order).getId());
        payload.put("reason", reason);
        return payload;
    }

    private Map<String, Object> bookDeltaPayload(EngineResult.BookDelta bookDelta) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", bookDelta.reason().name());
        payload.put("side", bookDelta.delta().side().name());
        payload.put("price", plain(bookDelta.delta().price()));
        payload.put("beforeTotalQty", plain(bookDelta.delta().before().totalQty()));
        payload.put("beforeOrderCount", bookDelta.delta().before().orderCount());
        payload.put("afterTotalQty", plain(bookDelta.delta().after().totalQty()));
        payload.put("afterOrderCount", bookDelta.delta().after().orderCount());
        return payload;
    }

    private Map<String, Object> tradePayload(Trade trade) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tradeKey", trade.getTradeKey());
        payload.put("makerOrderSide", trade.getMakerOrderSide().name());
        payload.put("price", plain(trade.getPrice()));
        payload.put("quantity", plain(trade.getQuantity()));
        payload.put("quoteAmount", plain(trade.getQuoteAmount()));
        return payload;
    }

    // 주문 처리 지갑 반환.
    private Wallet requireSourceWallet(Map<WalletKey, Wallet> walletsByKey, Order order) {
        Asset sourceAsset = sourceAsset(order);
        Wallet wallet = walletsByKey.get(WalletKey.of(order.getUser().getId(), sourceAsset.getId()));
        if (wallet != null) {
            return wallet;
        }

        throw new BusinessException(order.getOrderSide() == OrderSide.BID
                ? ErrorCode.QUOTE_WALLET_NOT_FOUND
                : ErrorCode.BASE_WALLET_NOT_FOUND);
    }

    // 타겟 지갑 호출 및 생성.
    private Wallet getOrCreateTargetWallet(Map<WalletKey, Wallet> walletsByKey, Order order) {
        Asset targetAsset = targetAsset(order);
        WalletKey key = WalletKey.of(order.getUser().getId(), targetAsset.getId());
        Wallet existing = walletsByKey.get(key);
        if (existing != null) {
            return existing;
        }

        Wallet created = Wallet.create(order.getUser(), targetAsset, BigDecimal.ZERO, BigDecimal.ZERO);
        walletsByKey.put(key, created);
        return created;
    }

    // order side에 맞는 자산 반환.
    private Asset sourceAsset(Order order) {
        return order.getOrderSide() == OrderSide.BID
                ? order.getMarket().getQuoteAsset()
                : order.getMarket().getBaseAsset();
    }

    private Asset targetAsset(Order order) {
        return order.getOrderSide() == OrderSide.BID
                ? order.getMarket().getBaseAsset()
                : order.getMarket().getQuoteAsset();
    }

    private String tradeKey(Order taker, int index) {
        return taker.getOrderKey() + "-trade-" + (index + 1);
    }

    private String eventId(Long commandLogId, int eventSequence) {
        return commandLogId + "-event-" + eventSequence;
    }

    private String resolveCancelReason(EngineResult.CancelReason cancelReason) {
        return cancelReason == null ? null : cancelReason.name();
    }

    private String unlockReason(EngineResult.PlaceResult result) {
        String reason = resolveCancelReason(result.cancelReason());
        return reason == null ? "MATCH_REMAINDER_UNLOCK" : reason;
    }

    private String orderUnlockIdempotencyKey(Order order, Long commandLogId) {
        return "order:" + order.getOrderKey() + ":unlock:" + commandLogId;
    }

    private String tradeLedgerIdempotencyKey(Trade trade, String leg) {
        return "trade:" + trade.getTradeKey() + ":" + leg;
    }

    private BigDecimal remainingLockedAmount(Order order) {
        if (order.getOrderSide() == OrderSide.BID) {
            if (order.getOrderType() == OrderType.MARKET) {
                return requiredAmount(order.getQuoteAmount(), "market bid 주문의 quoteAmount가 없습니다.")
                        .subtract(order.getExecutedQuoteAmount());
            }
            return roundDownQuote(
                    requiredAmount(order.getPrice(), "limit bid 주문의 price가 없습니다.")
                            .multiply(remainingQuantity(order)),
                    order
            );
        }
        return remainingQuantity(order);
    }

    private BigDecimal roundDownQuote(BigDecimal value, Order order) {
        return value.setScale(order.getMarket().getQuoteAsset().getDecimals().intValue(), RoundingMode.DOWN);
    }

    private BigDecimal remainingQuantityForStatusPayload(Order order) {
        if (order.getOrderType() == OrderType.MARKET && order.getOrderSide() == OrderSide.BID) {
            return null;
        }
        return remainingQuantity(order);
    }

    private BigDecimal remainingQuantity(Order order) {
        return requiredAmount(order.getQuantity(), "remaining quantity를 계산할 수 없는 주문입니다.")
                .subtract(order.getExecutedQuantity());
    }

    private BigDecimal requiredAmount(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String plainNullable(BigDecimal value) {
        return value == null ? null : plain(value);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "worker event payload 직렬화에 실패했습니다.");
        }
    }

    private record WalletKey(Long userId, Long assetId) {

        private static WalletKey from(Wallet wallet) {
            return of(wallet.getUser().getId(), wallet.getAsset().getId());
        }

        private static WalletKey of(Long userId, Long assetId) {
            return new WalletKey(userId, assetId);
        }
    }

    private record WalletMutationSnapshots(
            LedgerWriteService.BalanceSnapshot before,
            LedgerWriteService.BalanceSnapshot after
    ) {
    }

    private record PendingTradeLedgerWrite(
            Wallet wallet,
            LedgerWriteService.BalanceSnapshot beforeSnapshot,
            LedgerWriteService.BalanceSnapshot afterSnapshot,
            ChangeType changeType,
            BigDecimal amount,
            Trade trade,
            String description,
            String idempotencyKey
    ) {
    }

    private record PendingOrderLedgerWrite(
            Wallet wallet,
            LedgerWriteService.BalanceSnapshot beforeSnapshot,
            LedgerWriteService.BalanceSnapshot afterSnapshot,
            LedgerType ledgerType,
            ChangeType changeType,
            BigDecimal amount,
            Order order,
            String description,
            String idempotencyKey
    ) {
    }
}
