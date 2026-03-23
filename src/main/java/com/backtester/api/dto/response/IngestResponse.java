package com.backtester.api.dto.response;

/**
 * Response DTO returned after a bar ingestion operation.
 *
 * @param ticker       Ticker symbol bars were ingested for.
 * @param barsIngested Number of new bars saved to the database.
 * @param barsSkipped  Number of bars omitted because they already existed (duplicate date).
 */
public record IngestResponse(String ticker, int barsIngested, int barsSkipped) {}
