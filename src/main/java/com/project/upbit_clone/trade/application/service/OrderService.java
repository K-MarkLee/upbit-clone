package com.project.upbit_clone.trade.application.service;

import com.project.upbit_clone.global.domain.vo.EnumStatus;
import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.model.Market;
import com.project.upbit_clone.trade.domain.model.Order;
import com.project.upbit_clone.trade.domain.repository.MarketRepository;
import com.project.upbit_clone.trade.domain.repository.OrderRepository;
import com.project.upbit_clone.trade.domain.vo.OrderSide;
import com.project.upbit_clone.user.domain.model.User;
import com.project.upbit_clone.user.domain.repository.UserRepository;
import com.project.upbit_clone.wallet.domain.model.Wallet;
import com.project.upbit_clone.wallet.domain.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final WalletRepository walletRepository;


    // 주문 생성 전 사전조건 (사용자 / 시장 / 지갑) 을 조회하고 검증한다.
    @Transactional
    public OrderPrecondition prepareOrderPreconditions(PreconditionCommand command) {
        // 커맨드 null / blank검증
        validatePreconditionCommand(command);

        // 조회 및 검증
        User user = loadActiveUser(command.userId());
        Market market = loadActiveMarket(command.marketId());
        MarketAssetIds marketAssetIds = extractMarketAssetIds(market);

        // 이미 주문이 된건지 멱등성 검증
        Optional<Order> existingOrder = findExistingOrder(command.userId(), command.clientOrderId());
        if (existingOrder.isPresent()) {
            return OrderPrecondition.fromExisting(existingOrder.get());
        }

        // 자산의 지갑을 조회하고 검증 ( quote는 필수 , base는 없으면 생성)
        Wallet quoteWallet = loadRequiredQuoteWallet(command.userId(), marketAssetIds.quoteAssetId());
        Wallet baseWallet = loadBaseWalletBySide(
                command.side(),
                user,
                market,
                marketAssetIds.baseAssetId()
        );

        // 사전조건 통과반환
        return OrderPrecondition.forCreate(user, market, quoteWallet, baseWallet);
    }

    // 사전조건 커맨드의 필수값 null / blank값을 검증한다.
    private static void validatePreconditionCommand(PreconditionCommand command) {
        if (command == null
                || command.userId() == null
                || command.marketId() == null
                || command.side() == null
                || command.clientOrderId() == null
                || command.clientOrderId().isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_VALUE);
        }
    }

    // 주문을 요청한 사용자를 조회하고, 주문이 가능한 상태인지 검증한다.
    private User loadActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != EnumStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_ALLOWED_USER_STATUS);
        }
        return user;
    }

    // 주문이 요청된 시장을 조회하고, 거래 가능한 상태인지 검증한다.
    private Market loadActiveMarket(Long marketId) {
        Market market = marketRepository.findById(marketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MARKET_NOT_FOUND));

        if (market.getStatus() != EnumStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_ALLOWED_MARKET_STATUS);
        }
        return market;
    }

    // 해당 시장의 quote / base 자산을 추출한다.
    private MarketAssetIds extractMarketAssetIds(Market market) {
        Long quoteAssetId = market.getQuoteAsset().getId();
        Long baseAssetId = market.getBaseAsset().getId();

        return new MarketAssetIds(quoteAssetId, baseAssetId);
    }

    // 동일한 clientOrderId로 접수된 기존의 주문을 조회한다. (멱등성 히트)
    private Optional<Order> findExistingOrder(Long userId, String clientOrderId) {
        return orderRepository.findByUserIdAndClientOrderId(userId, clientOrderId);
    }

    // 거래에 필요한 quote 지갑이 존재하는지 검증 / 반환한다.
    private Wallet loadRequiredQuoteWallet(Long userId, Long quoteAssetId) {
        return walletRepository.findByUserIdAndAssetId(userId, quoteAssetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTE_WALLET_NOT_FOUND));
    }

    // 주문 방향에 따라 base 지갑 정책을 적용한다.
    private Wallet loadBaseWalletBySide(OrderSide side, User user, Market market, Long baseAssetId) {
        Optional<Wallet> baseWallet = walletRepository.findByUserIdAndAssetId(user.getId(), baseAssetId);
        // 매수면 거래에 필요한 base 지갑이 없으면 생성
        if (side == OrderSide.BID) {
            return baseWallet.orElseGet(() -> walletRepository.save(
                            Wallet.create(user, market.getBaseAsset(), BigDecimal.ZERO, BigDecimal.ZERO))
                    );
        }
        // 매도면 거래에 필요한 base 지갑이 존재하는지 검증 / 반환
        if (side == OrderSide.ASK) {
            return baseWallet.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_WALLET_INPUT));
        }
        throw new BusinessException(ErrorCode.INVALID_ORDER_SIDE);
    }

    // 필수값
    public record PreconditionCommand(
            Long userId,
            Long marketId,
            OrderSide side,
            String clientOrderId
    ) {
    }

    // 주문 반환 정보
    public record OrderPrecondition(
            Order existingOrder,
            User user,
            Market market,
            Wallet quoteWallet,
            Wallet baseWallet
    ) {

        // 멱등성 키 조회
        public boolean isIdempotentHit() {
            return existingOrder != null;
        }


        // 멱등성 히트시, 반환정보를 설정하고 반환한다.
        private static OrderPrecondition fromExisting(Order existingOrder) {
            return new OrderPrecondition(existingOrder, null, null, null, null);
        }


        // 신규 주문을 생성하고 결과를 반환한다.
        private static OrderPrecondition forCreate(User user, Market market, Wallet quoteWallet, Wallet baseWallet) {
            return new OrderPrecondition(null, user, market, quoteWallet, baseWallet);
        }
    }

    private record MarketAssetIds(Long quoteAssetId, Long baseAssetId) {
    }
}
