package com.backtester.domain.backtest;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EquityCurvePoint(LocalDate date, BigDecimal equity) {}
