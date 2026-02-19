package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
public class CreateOrder {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;

    /*
     * 주문 생성 트랜잭션.
     * validate 단계 통과를 전제로 주문 생성, 지갑 락 반영, 원장 기록을 수행한다.
     */
    @Transactional
    public Order createOrder(OrderOrchestrator.PlaceOrderCommand command) {

        // validate에서 user/market 존재 및 상태는 이미 검증됨.
        User user = userRepository.getReferenceById(command.userId());
        Market market = marketRepository.getReferenceById(command.marketId());

        Order order = Order.create(new Order.CreateCommand(
                market,
                user,
                command.clientOrderId(),
                command.side(),
                command.orderType(),
                command.timeInForce(),
                command.price(),
                command.quantity(),
                command.quoteAmount()
        ));

        // 지갑, 금액 검증
        Wallet lockWallet = loadWallet(order);
        BigDecimal lockAmount = calculateLockAmount(order);
        BigDecimal availableBefore = lockWallet.getAvailableBalance();
        BigDecimal lockedBefore = lockWallet.getLockedBalance();

        // available -> locked
        lockWallet.lock(lockAmount);

        // 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 원장 생성
        ledgerRepository.save(createOrderLockLedger(
                savedOrder,
                lockWallet,
                lockAmount,
                availableBefore,
                lockedBefore
        ));

        return savedOrder;
    }

    // 주문 방향에 따라 락 대상 지갑(quote/base)을 조회한다.
    private Wallet loadWallet(Order order) {
        Long userId = order.getUser().getId();

        if (order.getOrderSide() == OrderSide.BID) {
            return walletRepository.findByUserIdAndAssetId(userId, order.getMarket().getQuoteAsset().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.QUOTE_WALLET_NOT_FOUND));
        }

        if (order.getOrderSide() == OrderSide.ASK) {
            return walletRepository.findByUserIdAndAssetId(userId, order.getMarket().getBaseAsset().getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BASE_WALLET_NOT_FOUND));
        }


        throw new BusinessException(ErrorCode.INVALID_ORDER_SIDE);
    }

    // 주문 타입/방향에 따라 락할 수량(또는 금액)을 계산한다.
    private static BigDecimal calculateLockAmount(Order order) {
        if (order.getOrderSide() == OrderSide.BID) {
            if (order.getOrderType() == OrderType.LIMIT) {
                return order.getPrice().multiply(order.getQuantity());
            }
            if (order.getOrderType() == OrderType.MARKET) {
                return order.getQuoteAmount();
            }
            throw new BusinessException(ErrorCode.INVALID_ORDER_INPUT);
        }

        if (order.getOrderSide() == OrderSide.ASK) {
            return order.getQuantity();
        }

        throw new BusinessException(ErrorCode.INVALID_ORDER_INPUT);
    }

    // 원장 생성
    private static Ledger createOrderLockLedger(
            Order order,
            Wallet wallet,
            BigDecimal lockAmount,
            BigDecimal availableBefore,
            BigDecimal lockedBefore
    ) {
        return Ledger.create(new Ledger.CreateCommand(
                wallet,
                wallet.getAsset(),
                LedgerType.ORDER_LOCK,
                ChangeType.DECREASE,
                lockAmount,
                availableBefore,
                wallet.getAvailableBalance(),
                lockedBefore,
                wallet.getLockedBalance(),
                ReferenceType.ORDER,
                order.getId(),
                null,
                buildOrderLockIdempotencyKey(order.getId())
        ));
    }

    // 멱등성 키를 생성.
    private static String buildOrderLockIdempotencyKey(Long orderId) {
        return "ORDER_LOCK:" + orderId;
    }
}
