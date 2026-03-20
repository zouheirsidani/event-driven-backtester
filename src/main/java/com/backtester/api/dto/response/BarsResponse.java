package com.backtester.api.dto.response;

import java.util.List;

public record BarsResponse(String ticker, List<BarDto> bars, int count, long totalCount, int page, int size) {}
