package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.model.Trade;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.repository.TradeRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderStatus;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import com.project.upbit_clone.wallet.domain.model.Ledger;
import com.project.upbit_clone.wallet.domain.model.Wallet;
import com.project.upbit_clone.wallet.domain.repository.LedgerRepository;
import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import com.project.upbit_clone.wallet.domain.vo.ChangeType;
import com.project.upbit_clone.wallet.domain.vo.LedgerType;
import com.project.upbit_clone.wallet.domain.vo.ReferenceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class MatchingService {

    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final TradeRepository tradeRepository;

    private static final String CANCEL_REASON_IOC_DUST_REMAINDER = "IOC_DUST_REMAINDER";

    @Transactional
    public void match(Long takerOrderId) {
        // 입력값 null체크
        Objects.requireNonNull(takerOrderId, "takerOrderId must not be null");

        // OPEN 상태의 주문만 반환한다.
        Optional<Order> takerOptional = loadOpenTakerOrder(takerOrderId);
        if (takerOptional.isEmpty()) {
            return;
        }
        Order taker = takerOptional.get();

        // taker가 OPEN상태일떄 반복
        while (isOpen(taker)) {
            // 최적의 maker 가격 찾기. ( 가격 정렬 + 시간 정렬 )
            Optional<Order> makerOptional = findBestMakerOrder(taker);
            if (makerOptional.isEmpty()) {
                // maker가 없을때, MARKET 거래는 주문을 닫고 이유 반환.
                if (shouldCancelOnNoMatch(taker)) {
                    cancelAndUnlock(taker, "IOC_NO_LIQUIDITY");
                }
                return;
            }
            Order maker = makerOptional.get();

            // 가격 검증 ( 최적 가격에 거래가능한지 )
            if (!isPriceCrossed(taker, maker)) {
                if (shouldCancelOnNoMatch(taker)) {
                    cancelAndUnlock(taker, "PRICE_NOT_MATCHED");
                }
                return;
            }

            // 한 번의 매칭을 반영한다. (한건의 거래 마다 기록 - 자산 갯수 단위가 아닌 거래단위)
            boolean matched = executeOneMatch(taker, maker);
            if (!matched) {
                return;
            }

            // 다음 루프를 위해 최신 taker 상태를 재조회한다.
            taker = reloadTaker(taker.getId()).orElse(taker);

        }
    }

    // taker를 조회하고 OPEN 상태인지 확인한다.
    private Optional<Order> loadOpenTakerOrder(Long takerOrderId) {
        return orderRepository.findById(takerOrderId)
                .filter(this::isOpen);
    }

    // 주문 상태가 OPEN인지 확인한다.
    private boolean isOpen(Order order) {
        return order != null && order.getStatus() == OrderStatus.OPEN;
    }

    // 반대편 best maker를 조회한다.
    private Optional<Order> findBestMakerOrder(Order taker) {
        Long marketId = taker.getMarket().getId();
        Long takerUserId = taker.getUser().getId();

        // BID taker -> ASK maker(최저가/선주문 우선)
        if (taker.getOrderSide() == OrderSide.BID) {
            return orderRepository.findFirstByMarketIdAndStatusAndOrderSideAndUserIdNot(
                    marketId,
                    OrderStatus.OPEN,
                    OrderSide.ASK,
                    takerUserId,
                    OrderRepository.asksort
            );
        }

        // ASK taker -> BID maker(최고가/선주문 우선)
        return orderRepository.findFirstByMarketIdAndStatusAndOrderSideAndUserIdNot(
                marketId,
                OrderStatus.OPEN,
                OrderSide.BID,
                takerUserId,
                OrderRepository.bidsort
        );
    }

    // taker 가 시장가 거래인지 확인한다.
    private boolean shouldCancelOnNoMatch(Order taker) {
        return taker.getOrderType() == OrderType.MARKET || taker.getTimeInForce() == TimeInForce.IOC;
    }

    // 자산을 unlock 시키고 이유를 추가하고, 원장을 생성한다.
    private void cancelAndUnlock(Order taker, String reason) {

        // 잔여 lock 호출
        BigDecimal remainderLock = calculateRemainderLock(taker);

        // 상태가 open 인데 잔여락이 0
        // ? 근데 체결이 안된 주문인데 0 이 걸릴수가 있나? 걸리면 문제아닌가?
        if (remainderLock.compareTo(BigDecimal.ZERO) <= 0) {
            taker.cancel(CANCEL_REASON_IOC_DUST_REMAINDER);
            return;
        }

        // 잔여 락은 decimals기준으로 정규화.
        Wallet lockWallet = loadLockWallet(taker);
        int decimals = lockWallet.getAsset().getDecimals();
        BigDecimal unlockAmount = remainderLock.setScale(decimals, RoundingMode.DOWN);

        // ? 이것도 이미 rounding다 한 값을 반환하려고 다시 정규화 했는데 0이 되는거면 원래 주문이 틀린거아닌가?
        if (unlockAmount.compareTo(BigDecimal.ZERO) <= 0) {
            taker.cancel(CANCEL_REASON_IOC_DUST_REMAINDER);
            return;
        }

        // before값 생성.
        BigDecimal availableBefore = lockWallet.getAvailableBalance();
        BigDecimal lockedBefore = lockWallet.getLockedBalance();

        // 자산 locked -> available
        lockWallet.unlock(unlockAmount);

        // 원장 생성
        ledgerRepository.save(createOrderUnlockLedger(
                taker,
                lockWallet,
                unlockAmount,
                availableBefore,
                lockedBefore
        ));

        // 실패 이유 ("IOC_NO_LIQUIDITY")
        taker.cancel(reason);
    }

    // 남아있는(반환해야하는) 락 계산 ( 주문 값 - 체결 값 )
    private BigDecimal calculateRemainderLock(Order taker) {
        // ASK : 주문 수량 - 체결 수량
        if (taker.getOrderSide() == OrderSide.ASK) {
            return nonNegative(taker.getQuantity().subtract(taker.getExecutedQuantity()));
        }

        // MARKET - BID : 주문 금액 - 체결 금액
        if (taker.getOrderType() == OrderType.MARKET) {
            return nonNegative(taker.getQuoteAmount().subtract(taker.getExecutedQuoteAmount()));
        }

        // LIMIT - BID : 초기 락 - 체결 금액
        BigDecimal initialLock = taker.getPrice().multiply(taker.getQuantity());
        return nonNegative(initialLock.subtract(taker.getExecutedQuoteAmount()));
    }

    // 주문 방향에 따라 락 대상 지갑(quote/base)을 조회한다.
    private Wallet loadLockWallet(Order taker) {
        Long userId = taker.getUser().getId();
        Long quoteMarketId = taker.getMarket().getQuoteAsset().getId();
        Long baseMarketId = taker.getMarket().getBaseAsset().getId();

        // 매수면, quote 지갑
        if (taker.getOrderSide() == OrderSide.BID) {
            return walletRepository.findByUserIdAndAssetId(userId, quoteMarketId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.QUOTE_WALLET_NOT_FOUND));
        }

        // 매도면, base 지갑
        if (taker.getOrderSide() == OrderSide.ASK) {
            return walletRepository.findByUserIdAndAssetId(userId, baseMarketId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.BASE_WALLET_NOT_FOUND));
        }

        throw new BusinessException(ErrorCode.INVALID_ORDER_SIDE);

    }

    // taker와 maker가 체결 가능한 가격인지 확인한다.
    private boolean isPriceCrossed(Order taker, Order maker) {
        // 시장가는 가격 제한 없이 체결 가능
        if (taker.getOrderType() == OrderType.MARKET) {
            return true;
        }

        // 지정가 매수: taker.price >= maker.price
        if ( taker.getOrderSide() == OrderSide.BID) {
            return taker.getPrice().compareTo(maker.getPrice()) >= 0;
        }

        // 지정가 매도: taker.price <= maker.price
        return taker.getPrice().compareTo(maker.getPrice()) <= 0;
    }

    // taker 와 maker의 거래를 체결한다.
    private boolean executeOneMatch(Order taker, Order maker) {

        // 거래 가능 수량, 거래 가능 금액 그리고 maker의 가격을 호출.
        MatchAmount matchAmount = calculateMatchAmount(taker, maker);

        // 체결 불가시
        // IOC 면 취소 아니면 체결 실패.
        if (matchAmount.isZero()) {
            if (shouldCancelOnNoMatch(taker)) {
                cancelAndUnlock(taker, CANCEL_REASON_IOC_DUST_REMAINDER);
            }
            return false;
        }

        // 구매주문과 판매주문 구별
        Order buyOrder = (taker.getOrderSide() == OrderSide.BID) ? taker : maker;
        Order sellOrder = (taker.getOrderSide() == OrderSide.ASK) ? taker : maker;

        // 거래 체결
        // TODO : feerate계산 미구현
        Trade trade = tradeRepository.save(Trade.create(new Trade.CreateCommand(
                taker.getMarket(),
                buyOrder,
                sellOrder,
                maker.getOrderSide(),
                matchAmount.price(),
                matchAmount.quantity(),
                matchAmount.quoteAmount(),
                null,         // feeRate 은 기본 0.0005
                BigDecimal.ZERO,     // buyFee
                BigDecimal.ZERO      // sellFee
        )));

        // 원장 생성
        settleAndRecordLedger(trade, buyOrder, sellOrder, matchAmount);

        // 주문 업데이트 ( 체결 금액 추가 )
        buyOrder.applyExecutedQuantity(matchAmount.quantity(), matchAmount.quoteAmount());
        sellOrder.applyExecutedQuantity(matchAmount.quantity(), matchAmount.quoteAmount());
        return true;
    }

    // 체결 가격을 계산한다.
    private MatchAmount calculateMatchAmount(Order taker, Order maker) {

        BigDecimal makerPrice = maker.getPrice();
        int baseScale = taker.getMarket().getBaseAsset().getDecimals();
        int quoteScale = taker.getMarket().getQuoteAsset().getDecimals();

        // 거래 가능 수량 / 금액
        BigDecimal makerRemainingQty = remainingQuantity(maker);
        BigDecimal takerRemainingQuote = remainingQuote(taker);

        BigDecimal takerTargetQty;

        // MARKET-BID : 구매가능한 수량 ( 남은 거래 가능 금액 / maker의 가격 )
        // els : 구매가능한 수량 ( 거래 가능 수량 )
        if (taker.getOrderType() == OrderType.MARKET && taker.getOrderSide() == OrderSide.BID) {
            takerTargetQty = takerRemainingQuote.divide(makerPrice, baseScale, RoundingMode.DOWN);
        } else {
            takerTargetQty = remainingQuantity(taker);
        }

        // 거래 가능 수량 : 구매 가능 수량 과 거래 가능 수량중 작은 값.
        BigDecimal tradeQty = takerTargetQty.min(makerRemainingQty).setScale(baseScale, RoundingMode.DOWN);
        if (tradeQty.compareTo(BigDecimal.ZERO) <= 0) {
            return MatchAmount.zero();
        }

        // 거래 가능 금액 : 정규화( 거래 가능 수량 * price )
        BigDecimal tradeQuote = tradeQty.multiply(makerPrice).setScale(quoteScale, RoundingMode.DOWN);
        if (tradeQuote.compareTo(BigDecimal.ZERO) <= 0) {
            return MatchAmount.zero();
        }

        // 거래 가능 수량, 거래 가능 금액 그리고 maker의 가격을 반환.
        return new MatchAmount(tradeQty, tradeQuote, makerPrice);
    }

    // 거래 가능 수량 반환
    private BigDecimal remainingQuantity(Order order) {
        if (order.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        // 이미 체결된 수량 제외 후 반환
        return nonNegative(order.getQuantity().subtract(order.getExecutedQuantity()));
    }

    // 거래 가능 금액 반환
    private BigDecimal remainingQuote(Order order) {
        if (order.getQuoteAmount() == null) {
            return BigDecimal.ZERO;
        }
        // 이미 체결된 금액 제외 후 반환
        return nonNegative(order.getQuoteAmount().subtract(order.getExecutedQuoteAmount()));
    }

    // 정산 및 원장 기록
    private void settleAndRecordLedger(Trade trade, Order buyOrder, Order sellOrder, MatchAmount amount) {
        Long buyerId = buyOrder.getUser().getId();
        Long sellerId = sellOrder.getUser().getId();
        Long baseAssetId = buyOrder.getMarket().getBaseAsset().getId();
        Long quoteAssetId = buyOrder.getMarket().getQuoteAsset().getId();

        // 구매자 지갑 호출
        Wallet buyerQuote = walletRepository.findByUserIdAndAssetId(buyerId, quoteAssetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTE_WALLET_NOT_FOUND));
        Wallet buyerBase = walletRepository.findByUserIdAndAssetId(buyerId, baseAssetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BASE_WALLET_NOT_FOUND));

        // 판매자 지갑 호출
        Wallet sellerQuote = walletRepository.findByUserIdAndAssetId(sellerId, quoteAssetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTE_WALLET_NOT_FOUND));
        Wallet sellerBase = walletRepository.findByUserIdAndAssetId(sellerId, baseAssetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BASE_WALLET_NOT_FOUND));

        // buyer quote locked 감소
        BigDecimal bqAvailBefore = buyerQuote.getAvailableBalance();
        BigDecimal bqLockBefore = buyerQuote.getLockedBalance();
        buyerQuote.decreaseLocked(amount.quoteAmount());
        ledgerRepository.save(createTradeLedger(trade, buyerQuote, ChangeType.DECREASE, amount.quoteAmount(), bqAvailBefore, bqLockBefore, "BUYER_QUOTE_OUT"));

        // buyer base available 증가
        BigDecimal bbAvailBefore = buyerBase.getAvailableBalance();
        BigDecimal bbLockBefore = buyerBase.getLockedBalance();
        buyerBase.increaseAvailable(amount.quantity());
        ledgerRepository.save(createTradeLedger(trade, buyerBase, ChangeType.INCREASE, amount.quantity(), bbAvailBefore, bbLockBefore, "BUYER_BASE_IN"));

        // seller base locked 감소
        BigDecimal sbAvailBefore = sellerBase.getAvailableBalance();
        BigDecimal sbLockBefore = sellerBase.getLockedBalance();
        sellerBase.decreaseLocked(amount.quantity());
        ledgerRepository.save(createTradeLedger(trade, sellerBase, ChangeType.DECREASE, amount.quantity(), sbAvailBefore, sbLockBefore, "SELLER_BASE_OUT"));

        // seller quote available 증가
        BigDecimal sqAvailBefore = sellerQuote.getAvailableBalance();
        BigDecimal sqLockBefore = sellerQuote.getLockedBalance();
        sellerQuote.increaseAvailable(amount.quoteAmount());
        ledgerRepository.save(createTradeLedger(trade, sellerQuote, ChangeType.INCREASE, amount.quoteAmount(), sqAvailBefore, sqLockBefore, "SELLER_QUOTE_IN"));
    }

    // value가 0보다 작거나 같으면 0을 반환.
    private BigDecimal nonNegative(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0 ? value : BigDecimal.ZERO;
    }

    // 매칭 후 최신 taker 상태를 다시 조회한다.
    private Optional<Order> reloadTaker(Long takerOrderId) {
        return orderRepository.findById(takerOrderId);
    }

    // MatchAmount 구조
    private record MatchAmount(BigDecimal quantity, BigDecimal quoteAmount, BigDecimal price) {
        static MatchAmount zero() {
            return new MatchAmount(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        boolean isZero() {
            return quantity.compareTo(BigDecimal.ZERO) == 0 || quoteAmount.compareTo(BigDecimal.ZERO) == 0;
        }
    }

    // 원장 생성 ( locked -> available )
    private static Ledger createOrderUnlockLedger(
            Order order,
            Wallet wallet,
            BigDecimal unlockAmount,
            BigDecimal availableBefore,
            BigDecimal lockedBefore
    ) {
        return Ledger.create(new Ledger.CreateCommand(
                wallet,
                wallet.getAsset(),
                LedgerType.ORDER_UNLOCK,
                ChangeType.INCREASE,
                unlockAmount,
                availableBefore,
                wallet.getAvailableBalance(),
                lockedBefore,
                wallet.getLockedBalance(),
                ReferenceType.ORDER,
                order.getId(),
                null,
                "ORDER_UNLOCK:" + order.getId()
        ));
    }


    // 원장 생성 ( 거래 체결 )
    private Ledger createTradeLedger(
            Trade trade,
            Wallet wallet,
            ChangeType changeType,
            BigDecimal amount,
            BigDecimal availableBefore,
            BigDecimal lockedBefore,
            String flow
    ) {
        return Ledger.create(new Ledger.CreateCommand(
                wallet,
                wallet.getAsset(),
                LedgerType.TRADE,
                changeType,
                amount,
                availableBefore,
                wallet.getAvailableBalance(),
                lockedBefore,
                wallet.getLockedBalance(),
                ReferenceType.TRADE,
                trade.getId(),
                null,
                "TRADE:" + trade.getId() + ":" + flow
        ));
    }

}
