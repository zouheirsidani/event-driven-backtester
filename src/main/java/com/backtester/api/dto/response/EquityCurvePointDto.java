package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EquityCurvePointDto(LocalDate date, BigDecimal equity) {}
