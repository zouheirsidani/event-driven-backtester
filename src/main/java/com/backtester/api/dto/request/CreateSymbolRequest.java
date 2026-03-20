package com.backtester.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSymbolRequest(
        @NotBlank(message = "ticker is required") String ticker,
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "exchange is required") String exchange,
        @NotNull(message = "assetClass is required") String assetClass
) {}
