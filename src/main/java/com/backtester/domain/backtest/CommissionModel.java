package com.backtester.domain.backtest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FixedCommission.class, name = "FIXED"),
        @JsonSubTypes.Type(value = PerShareCommission.class, name = "PER_SHARE")
})
public sealed interface CommissionModel permits FixedCommission, PerShareCommission {}
