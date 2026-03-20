package com.backtester.domain.event;

import com.backtester.domain.strategy.SignalDirection;

import java.math.BigDecimal;
import java.time.Instant;

public record SignalEvent(
        String ticker,
        SignalDirection direction,
        BigDecimal strength,
        String strategyId,
        Instant timestamp
) implements TradingEvent {}
