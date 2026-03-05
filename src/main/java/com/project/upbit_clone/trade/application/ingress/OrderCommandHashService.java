package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
import com.project.upbit_clone.trade.domain.vo.OrderType;
import com.project.upbit_clone.trade.domain.vo.TimeInForce;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class OrderCommandHashService {

    public String hash(OrderCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.MISSING_ORDER_REQUIRED_VALUE);
        }
        String canonical = canonicalize(command);
        return sha256Hex(canonical);
    }

    // 멱등 키(userId, clientOrderId, commandType)를 제외하고 정규화 문자열 생성.
    private String canonicalize(OrderCommand command) {
        if (command instanceof PlaceOrder.Command place) {
            TimeInForce normalizedTif = normalizeTif(place.orderType(), place.timeInForce());
            return "marketId=" + place.marketId()
                    + "|orderSide=" + normalizeEnum(place.orderSide())
                    + "|orderType=" + normalizeEnum(place.orderType())
                    + "|timeInForce=" + normalizeEnum(normalizedTif)
                    + "|price=" + normalizeDecimal(place.price())
                    + "|quantity=" + normalizeDecimal(place.quantity())
                    + "|quoteAmount=" + normalizeDecimal(place.quoteAmount());
        }
        if (command instanceof CancelOrder.Command cancel) {
            return "marketId=" + cancel.marketId();
        }

        throw new BusinessException(ErrorCode.INVALID_COMMAND_TYPE);
    }

    private String normalizeEnum(Enum<?> value) {
        return value == null ? "null" : value.name();
    }

    private String normalizeDecimal(BigDecimal value) {
        if (value == null) {
            return "null";
        }
        // decimal의 소수점 아래 0 제거 1.2300 -> 1.23
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return normalized.toPlainString();
    }

    private TimeInForce normalizeTif(OrderType orderType, TimeInForce timeInForce) {
        if (orderType == OrderType.MARKET && (timeInForce == null || timeInForce == TimeInForce.IOC)) {
            return TimeInForce.IOC;
        }
        if (orderType == OrderType.LIMIT && (timeInForce == null || timeInForce == TimeInForce.GTC)) {
            return TimeInForce.GTC;
        }
        return timeInForce;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte h : hash) {
                builder.append(Character.forDigit((h >>> 4) & 0x0f, 16));
                builder.append(Character.forDigit(h & 0x0f, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "해시 생성에 실패했습니다.");
        }
    }
}
