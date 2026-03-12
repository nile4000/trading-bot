package ch.lueem.tradingbot.application;

/**
 * Holds application-level configuration loaded from YAML.
 */
public record ApplicationConfig(
        BacktestConfig backtest,
        ReportingConfig reporting,
        LoggingConfig logging
) {
}
