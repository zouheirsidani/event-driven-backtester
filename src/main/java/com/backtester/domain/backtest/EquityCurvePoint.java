package com.backtester.domain.backtest;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single data point on the portfolio equity curve.
 * The full list of these points, ordered by date, shows how portfolio value
 * evolved over the backtest period and is displayed as a line chart in the UI.
 *
 * @param date   Trading date.
 * @param equity Total portfolio equity (cash + position market values) at end of day.
 */
public record EquityCurvePoint(LocalDate date, BigDecimal equity) {}
