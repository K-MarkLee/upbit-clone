package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.global.exception.BusinessException;
import com.project.upbit_clone.global.exception.ErrorCode;
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

    // 이미 정규화된 command에서 멱등 해시용 canonical 문자열을 생성한다.
    private String canonicalize(OrderCommand command) {
        if (command instanceof PlaceOrder.Command place) {
            return "marketId=" + place.marketId()
                    + "|orderSide=" + normalizeEnum(place.orderSide())
                    + "|orderType=" + normalizeEnum(place.orderType())
                    + "|timeInForce=" + normalizeEnum(place.timeInForce())
                    + "|price=" + decimalString(place.price())
                    + "|quantity=" + decimalString(place.quantity())
                    + "|quoteAmount=" + decimalString(place.quoteAmount());
        }
        if (command instanceof CancelOrder.Command cancel) {
            return "marketId=" + cancel.marketId()
                    + "|cancelReason=" + normalizeText(cancel.cancelReason());
        }

        throw new BusinessException(ErrorCode.INVALID_COMMAND_TYPE);
    }

    private String normalizeText(String value) {
        return (value == null || value.isBlank()) ? "Order Canceled" : value.strip();
    }

    private String normalizeEnum(Enum<?> value) {
        return value == null ? "null" : value.name();
    }

    private String decimalString(BigDecimal value) {
        if (value == null) {
            return "null";
        }
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return value.toPlainString();
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
