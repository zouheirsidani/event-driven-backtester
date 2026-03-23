package com.backtester.api.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/backtests/compare}.
 * Specifies which completed backtest runs to compare side by side.
 *
 * @param runIds One or more UUIDs of completed backtest runs whose results to retrieve.
 */
public record CompareBacktestsRequest(
        @NotEmpty(message = "runIds must not be empty") List<UUID> runIds
) {}
