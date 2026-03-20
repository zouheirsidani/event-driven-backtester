package com.backtester.domain.backtest;

import java.math.BigDecimal;

public record FixedCommission(BigDecimal amount) implements CommissionModel {}
