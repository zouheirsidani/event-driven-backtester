package com.backtester.domain.event;

public sealed interface TradingEvent
        permits MarketDataEvent, SignalEvent, OrderEvent, FillEvent {}
