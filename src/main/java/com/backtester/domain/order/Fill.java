package com.backtester.domain.order;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The permanent record of a completed trade execution.
 * Fills are collected by the portfolio and stored in the backtest result
 * to support trade-level analytics (win rate, P&amp;L, profit factor, etc.).
 *
 * @param fillId         Unique identifier for this fill.
 * @param orderId        ID of the order that generated this fill.
 * @param ticker         Ticker that was traded.
 * @param side           BUY or SELL.
 * @param quantityFilled Number of shares executed.
 * @param fillPrice      Actual execution price (close price adjusted for slippage).
 * @param commission     Brokerage commission charged (already deducted from portfolio cash).
 * @param date           Trading date on which the fill occurred.
 */
public record Fill(
        String fillId,
        String orderId,
        String ticker,
        OrderSide side,
        int quantityFilled,
        BigDecimal fillPrice,
        BigDecimal commission,
        LocalDate date
) {}
