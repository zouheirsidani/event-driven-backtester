package com.backtester.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IngestBarsRequest(
        @NotBlank(message = "ticker is required") String ticker,
        @NotEmpty(message = "bars must not be empty") @Valid List<BarData> bars
) {

    public record BarData(
            @NotNull(message = "date is required") LocalDate date,
            @NotNull @Positive BigDecimal open,
            @NotNull @Positive BigDecimal high,
            @NotNull @Positive BigDecimal low,
            @NotNull @Positive BigDecimal close,
            @Positive long volume
    ) {}
}
