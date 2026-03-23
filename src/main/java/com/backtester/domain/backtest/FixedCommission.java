package com.backtester.domain.backtest;

import java.math.BigDecimal;

/**
 * Commission model that charges a flat fee per order fill, regardless of size.
 * Suitable for simulating retail brokers with a fixed ticket fee (e.g. $1.00/trade).
 *
 * @param amount Flat commission amount in dollars charged per fill.
 */
public record FixedCommission(BigDecimal amount) implements CommissionModel {}
