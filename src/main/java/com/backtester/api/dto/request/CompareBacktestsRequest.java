package com.backtester.api.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CompareBacktestsRequest(
        @NotEmpty(message = "runIds must not be empty") List<UUID> runIds
) {}
