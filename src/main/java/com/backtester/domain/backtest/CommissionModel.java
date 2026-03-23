package com.backtester.domain.backtest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed interface representing a commission model deducted from each order fill.
 * Commission reduces proceeds on SELL fills and increases cost on BUY fills.
 *
 * <p>Like {@link SlippageModel}, Jackson polymorphic typing is used so that
 * concrete implementations can be round-tripped through the PostgreSQL JSONB
 * {@code commission_config} column.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FixedCommission.class, name = "FIXED"),
        @JsonSubTypes.Type(value = PerShareCommission.class, name = "PER_SHARE")
})
public sealed interface CommissionModel permits FixedCommission, PerShareCommission {}
