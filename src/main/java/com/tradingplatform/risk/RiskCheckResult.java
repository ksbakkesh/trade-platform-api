package com.tradingplatform.risk;

import java.math.BigDecimal;

/**
 * Outcome of asking "can this account place a new trade right now?"
 * Carries enough detail to directly back the dashboard's risk summary
 * (BRD Section 13: Daily Trades Used, Daily Loss Used, Remaining Risk),
 * not just a yes/no.
 */
public record RiskCheckResult(
        boolean allowed,
        String reason,
        int tradesUsedToday,
        int maxTradesPerDay,
        BigDecimal lossUsedToday,
        BigDecimal dailyLossLimit
) {

    public BigDecimal remainingLossBudget() {
        BigDecimal remaining = dailyLossLimit.subtract(lossUsedToday);
        return remaining.signum() > 0 ? remaining : BigDecimal.ZERO;
    }

    public int remainingTrades() {
        int remaining = maxTradesPerDay - tradesUsedToday;
        return Math.max(remaining, 0);
    }
}
