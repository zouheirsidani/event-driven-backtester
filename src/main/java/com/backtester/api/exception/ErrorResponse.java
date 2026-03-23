package com.backtester.api.exception;

import java.time.Instant;

/**
 * Standardised error response body returned for all HTTP error responses.
 * Produced by {@link GlobalExceptionHandler} for every exception type it handles.
 *
 * @param status    HTTP status code (e.g. 400, 404, 500).
 * @param error     Short category label (e.g. "Not Found", "Validation Failed").
 * @param message   Detailed explanation from the exception message.
 * @param timestamp Wall-clock time when the error occurred.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp
) {}
