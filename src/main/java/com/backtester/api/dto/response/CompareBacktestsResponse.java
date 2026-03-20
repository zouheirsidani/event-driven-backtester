package com.backtester.api.dto.response;

import java.util.List;

public record CompareBacktestsResponse(List<BacktestResultResponse> results) {}
