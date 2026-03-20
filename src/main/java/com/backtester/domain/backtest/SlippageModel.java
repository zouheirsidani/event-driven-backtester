package com.backtester.domain.backtest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FixedSlippage.class, name = "FIXED"),
        @JsonSubTypes.Type(value = PercentSlippage.class, name = "PERCENT")
})
public sealed interface SlippageModel permits FixedSlippage, PercentSlippage {}
