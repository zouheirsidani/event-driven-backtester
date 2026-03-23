package com.backtester.api.exception;

/**
 * Unchecked exception thrown when a backtest operation fails for a domain-level
 * reason (e.g. invalid configuration, strategy not found).
 * Mapped to HTTP 400 Bad Request by {@link GlobalExceptionHandler}.
 */
public class BacktestException extends RuntimeException {

    /**
     * @param message Human-readable explanation of the failure.
     */
    public BacktestException(String message) {
        super(message);
    }
}
