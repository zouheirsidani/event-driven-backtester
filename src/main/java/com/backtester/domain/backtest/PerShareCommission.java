package com.backtester.domain.backtest;

import java.math.BigDecimal;

public record PerShareCommission(BigDecimal perShare) implements CommissionModel {}
