package com.backtester.domain.backtest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed interface representing a slippage model applied to every order fill.
 * Slippage simulates the difference between the theoretical close price and
 * the actual execution price due to market impact and bid-ask spread.
 *
 * <p>The {@code @JsonTypeInfo} / {@code @JsonSubTypes} annotations allow Jackson
 * to serialize and deserialize polymorphic instances into the PostgreSQL JSONB
 * {@code slippage_config} column using the {@code "type"} discriminator field.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FixedSlippage.class, name = "FIXED"),
        @JsonSubTypes.Type(value = PercentSlippage.class, name = "PERCENT")
})
public sealed interface SlippageModel permits FixedSlippage, PercentSlippage {}
