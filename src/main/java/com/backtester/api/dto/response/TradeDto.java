package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * API response DTO for a single executed trade fill.
 * Returned as part of {@link BacktestResultResponse#trades()}.
 *
 * @param fillId     Unique fill identifier.
 * @param ticker     Ticker symbol that was traded.
 * @param side       "BUY" or "SELL".
 * @param quantity   Number of shares traded.
 * @param price      Execution price after slippage.
 * @param commission Brokerage commission charged for this fill.
 * @param date       Trading date the fill occurred on.
 */
public record TradeDto(
        String fillId,
        String ticker,
        String side,
        int quantity,
        BigDecimal price,
        BigDecimal commission,
        LocalDate date
) {}
