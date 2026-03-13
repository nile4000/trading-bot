package ch.lueem.tradingbot.adapters.config;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Shared validation helpers for configuration records and loaders.
 */
public final class ConfigValidation {

    private ConfigValidation() {
    }

    public static void requirePresent(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    public static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }

    public static void requirePositive(long value, String message) {
        if (value <= 0L) {
            throw new IllegalStateException(message);
        }
    }

    public static void requirePositive(double value, String message) {
        if (value <= 0.0) {
            throw new IllegalStateException(message);
        }
    }

    public static void requirePositive(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalStateException(message);
        }
    }

    public static void requireNotEmpty(Collection<?> value, String message) {
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(message);
        }
    }
}
