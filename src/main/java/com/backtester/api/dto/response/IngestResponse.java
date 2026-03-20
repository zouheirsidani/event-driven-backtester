package com.backtester.api.dto.response;

public record IngestResponse(String ticker, int barsIngested, int barsSkipped) {}
