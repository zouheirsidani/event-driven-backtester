package com.backtester.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Point-in-time immutable snapshot of the portfolio taken at end-of-day.
 * Snapshots are collected by the event loop and used to build the equity
 * curve and to compute performance metrics.
 *
 * @param date        Trading date this snapshot was taken.
 * @param totalEquity Cash plus the market value of all open positions.
 * @param cash        Uninvested cash balance.
 * @param positions   Defensive copy of all open positions keyed by ticker.
 */
public record PortfolioSnapshot(
        LocalDate date,
        BigDecimal totalEquity,
        BigDecimal cash,
        Map<String, Position> positions
) {}
