package ch.lueem.tradingbot.adapters.execution.binance.order;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import ch.lueem.tradingbot.core.execution.Request;

/**
 * Creates stable Binance client order identifiers for one bot and market bar.
 */
public class BinanceClientOrderId {

    private static final String PREFIX = "tb-";
    private static final int PREFIX_HASH_LENGTH = 8;
    private static final int ORDER_HASH_LENGTH = 16;

    public String create(Request request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null.");
        }
        return prefix(request.runtimeId())
                + "-" + hash(payload(request), ORDER_HASH_LENGTH)
                + "-" + actionCode(request);
    }

    public String prefix(String runtimeId) {
        if (runtimeId == null || runtimeId.isBlank()) {
            throw new IllegalArgumentException("runtimeId must not be blank.");
        }
        return PREFIX + hash(runtimeId, PREFIX_HASH_LENGTH);
    }

    private String payload(Request request) {
        return String.join("|",
                request.runtimeId(),
                request.symbol(),
                request.timeframe(),
                request.requestedAt().toInstant().toString(),
                request.tradeAction().name());
    }

    private char actionCode(Request request) {
        return Character.toLowerCase(request.tradeAction().name().charAt(0));
    }

    private String hash(String value, int length) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, length);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
