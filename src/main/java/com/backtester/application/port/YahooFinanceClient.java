package com.backtester.application.port;

import com.backtester.domain.market.Bar;
import java.time.LocalDate;
import java.util.List;

/**
 * Port interface for fetching historical market data from an external source.
 * Implementations live in the infrastructure layer.
 *
 * <p>Application services depend on this interface, never on the concrete HTTP adapter,
 * keeping the domain and application layers free of infrastructure dependencies.
 */
public interface YahooFinanceClient {

    /**
     * Fetches daily OHLCV bars for the given ticker between from (inclusive) and to (inclusive).
     *
     * @param ticker the stock ticker symbol (e.g. "AAPL")
     * @param from   start date (inclusive)
     * @param to     end date (inclusive)
     * @return list of bars sorted by date ascending; never null
     * @throws RuntimeException if the external data source is unreachable or returns an error
     */
    List<Bar> fetchDailyBars(String ticker, LocalDate from, LocalDate to);
}
