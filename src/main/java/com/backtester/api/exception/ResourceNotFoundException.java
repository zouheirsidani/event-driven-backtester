package com.backtester.api.exception;

/**
 * Unchecked exception thrown when a requested resource (backtest run, symbol, result)
 * does not exist in the database.
 * Mapped to HTTP 404 Not Found by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message Human-readable description including the missing resource identifier.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
