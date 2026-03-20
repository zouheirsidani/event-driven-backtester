package com.backtester.domain.strategy;

import com.backtester.domain.event.SignalEvent;
import com.backtester.domain.market.Bar;
import com.backtester.domain.market.BarSeries;
import com.backtester.domain.portfolio.Portfolio;

import java.util.Optional;

public interface Strategy {

    String strategyId();

    String displayName();

    Optional<SignalEvent> onBar(BarSeries history, Bar currentBar, Portfolio portfolio);
}
