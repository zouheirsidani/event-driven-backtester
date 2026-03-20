package com.backtester.domain.event;

import com.backtester.domain.market.Bar;

import java.time.Instant;

public record MarketDataEvent(
        Bar bar,
        Instant timestamp
) implements TradingEvent {}
