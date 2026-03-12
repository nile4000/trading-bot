package ch.lueem.tradingbot.application;

/**
 * Holds application-level configuration loaded from YAML.
 */
public record ApplicationConfig(
        ApplicationMode mode,
        BacktestConfig backtest,
        PaperConfig paper,
        ReportingConfig reporting,
        LoggingConfig logging
) {
}
