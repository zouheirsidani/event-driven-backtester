package com.backtester.domain.backtest;

import java.math.BigDecimal;

public record PercentSlippage(BigDecimal percent) implements SlippageModel {}
