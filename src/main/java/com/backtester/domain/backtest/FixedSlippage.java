package com.backtester.domain.backtest;

import java.math.BigDecimal;

/**
 * Slippage model that adds or subtracts a flat dollar amount per share.
 * BUY fills are executed at {@code closePrice + amount}; SELL fills at
 * {@code closePrice - amount}.
 *
 * @param amount Fixed slippage amount per share in dollars (e.g. 0.01 = one cent).
 */
public record FixedSlippage(BigDecimal amount) implements SlippageModel {}
