package com.backtester.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * API response DTO for a single point on the portfolio equity curve.
 *
 * @param date   Trading date.
 * @param equity Total portfolio equity at end of day.
 */
public record EquityCurvePointDto(LocalDate date, BigDecimal equity) {}
