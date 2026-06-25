package com.tradingplatform.trade;

import com.tradingplatform.domain.Trade;

/**
 * Outcome of a trade execution attempt.
 * Carries enough detail so callers can decide on re-entry,
 * log the reason to system_logs, and update the dashboard.
 */
public record TradeExecutionResult(
        boolean executed,
        String reason,
        Trade trade          // null if not executed
) {
    public static TradeExecutionResult success(Trade trade) {
        return new TradeExecutionResult(true, "Order placed successfully", trade);
    }

    public static TradeExecutionResult blocked(String reason) {
        return new TradeExecutionResult(false, reason, null);
    }

    public static TradeExecutionResult failed(String reason) {
        return new TradeExecutionResult(false, reason, null);
    }
}
