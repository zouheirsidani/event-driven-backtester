package com.backtester.domain.backtest;

import java.math.BigDecimal;

public record FixedSlippage(BigDecimal amount) implements SlippageModel {}
