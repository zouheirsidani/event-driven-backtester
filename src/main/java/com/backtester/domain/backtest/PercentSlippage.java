package com.backtester.domain.backtest;

import java.math.BigDecimal;

/**
 * Slippage model that applies a percentage of the close price.
 * BUY fills are executed at {@code closePrice × (1 + percent)}; SELL fills at
 * {@code closePrice × (1 - percent)}.
 *
 * @param percent Slippage as a decimal fraction (e.g. 0.001 = 0.1%).
 */
public record PercentSlippage(BigDecimal percent) implements SlippageModel {}
