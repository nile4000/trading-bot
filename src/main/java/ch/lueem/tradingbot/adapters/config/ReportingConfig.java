package ch.lueem.tradingbot.adapters.config;

/**
 * Holds report rendering settings for the current application run.
 */
public record ReportingConfig(
        boolean prettyPrint,
        boolean includeNotes
) {
}
