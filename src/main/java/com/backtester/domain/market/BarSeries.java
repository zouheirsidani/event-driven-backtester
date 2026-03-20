package com.backtester.domain.market;

import java.util.List;

public record BarSeries(
        String ticker,
        List<Bar> bars
) {}
