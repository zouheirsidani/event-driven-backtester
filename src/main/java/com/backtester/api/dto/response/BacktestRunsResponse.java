package com.backtester.api.dto.response;

import java.util.List;

public record BacktestRunsResponse(List<BacktestRunDto> runs, int count, long totalCount, int page, int size) {}
