package ch.lueem.tradingbot.application;

/**
 * Holds report rendering settings for the current application run.
 */
public record ReportingConfig(
        String format,
        boolean prettyPrint,
        boolean includeNotes
) {
}
