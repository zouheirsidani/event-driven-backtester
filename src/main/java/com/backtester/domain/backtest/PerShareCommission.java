package com.backtester.domain.backtest;

import java.math.BigDecimal;

/**
 * Commission model that charges a fixed amount per share traded.
 * Total commission = {@code perShare × quantityFilled}.  Suitable for
 * simulating institutional brokers priced on a per-share basis (e.g. $0.005/share).
 *
 * @param perShare Commission amount in dollars charged for each individual share.
 */
public record PerShareCommission(BigDecimal perShare) implements CommissionModel {}
