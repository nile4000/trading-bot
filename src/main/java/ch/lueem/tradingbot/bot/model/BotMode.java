package ch.lueem.tradingbot.bot.model;

/**
 * Describes the trading execution reality used by backtest, paper and live flows.
 */
public enum BotMode {
    BACKTEST,
    PAPER,
    LIVE
}
