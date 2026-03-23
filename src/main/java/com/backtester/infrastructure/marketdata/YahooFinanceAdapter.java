package com.backtester.infrastructure.marketdata;

import com.backtester.application.port.YahooFinanceClient;
import com.backtester.domain.market.Bar;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fetches historical daily OHLCV bars from Yahoo Finance's unofficial chart API.
 * Parses the JSON response using {@link JsonNode} traversal to stay resilient to
 * minor schema changes.
 *
 * <p>URL template used:
 * {@code https://query1.finance.yahoo.com/v8/finance/chart/{ticker}?interval=1d&period1={start}&period2={end}}
 *
 * <p>This class is the infrastructure adapter that implements the
 * {@link YahooFinanceClient} port.  Application services depend only on the port
 * interface and are never coupled to this HTTP implementation.
 */
@Component
public class YahooFinanceAdapter implements YahooFinanceClient {

    private static final String YAHOO_URL =
        "https://query1.finance.yahoo.com/v8/finance/chart/{ticker}?interval=1d&period1={start}&period2={end}";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * @param restClient   Spring {@link RestClient} bean for outbound HTTP calls.
     * @param objectMapper Jackson mapper for JSON parsing.
     */
    public YahooFinanceAdapter(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches daily OHLCV bars from Yahoo Finance for the given ticker and date range.
     *
     * <p>The response timestamps are Unix epoch seconds in UTC; each is converted to a
     * {@link LocalDate}.  Entries where any OHLCV value is null (e.g. non-trading days
     * sometimes included by Yahoo) are silently skipped.
     *
     * @param ticker Ticker symbol (e.g. "AAPL").
     * @param from   Start date (inclusive).
     * @param to     End date (inclusive).
     * @return List of bars sorted by date ascending.
     * @throws RuntimeException if the HTTP call fails or the response cannot be parsed.
     */
    @Override
    public List<Bar> fetchDailyBars(String ticker, LocalDate from, LocalDate to) {
        long periodStart = from.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long periodEnd = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        String json;
        try {
            json = restClient.get()
                .uri(YAHOO_URL, ticker, periodStart, periodEnd)
                .header("User-Agent", "Mozilla/5.0")
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to fetch data from Yahoo Finance for ticker: " + ticker + ". " + e.getMessage(), e);
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode result = root.path("chart").path("result").get(0);
            if (result == null) {
                throw new RuntimeException("No data returned from Yahoo Finance for ticker: " + ticker);
            }

            JsonNode timestamps = result.path("timestamp");
            JsonNode quote = result.path("indicators").path("quote").get(0);

            JsonNode opens   = quote.path("open");
            JsonNode highs   = quote.path("high");
            JsonNode lows    = quote.path("low");
            JsonNode closes  = quote.path("close");
            JsonNode volumes = quote.path("volume");

            List<Bar> bars = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                // Skip non-trading day entries that Yahoo sometimes includes with null values
                if (opens.get(i).isNull() || highs.get(i).isNull() ||
                    lows.get(i).isNull() || closes.get(i).isNull() || volumes.get(i).isNull()) {
                    continue;
                }

                LocalDate date = Instant.ofEpochSecond(timestamps.get(i).asLong())
                    .atZone(ZoneOffset.UTC).toLocalDate();

                bars.add(new Bar(
                    ticker,
                    date,
                    BigDecimal.valueOf(opens.get(i).asDouble()),
                    BigDecimal.valueOf(highs.get(i).asDouble()),
                    BigDecimal.valueOf(lows.get(i).asDouble()),
                    BigDecimal.valueOf(closes.get(i).asDouble()),
                    volumes.get(i).asLong()
                ));
            }

            bars.sort(Comparator.comparing(Bar::date));
            return bars;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to parse Yahoo Finance response for ticker: " + ticker, e);
        }
    }
}
