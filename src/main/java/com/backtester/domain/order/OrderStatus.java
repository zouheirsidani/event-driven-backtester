package com.backtester.domain.order;

/**
 * Lifecycle state of an {@link Order}.
 * In the current synchronous event loop all orders are immediately filled or
 * discarded, so PENDING is transient and CANCELLED is only used for
 * informational or future-use purposes.
 */
public enum OrderStatus {
    /** Order has been created but not yet executed. */
    PENDING,
    /** Order was fully executed and a fill record was created. */
    FILLED,
    /** Order was not executed (e.g. insufficient data or cash). */
    CANCELLED
}
