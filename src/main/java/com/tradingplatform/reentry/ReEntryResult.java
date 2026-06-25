package com.tradingplatform.reentry;

/**
 * Outcome of evaluating whether a re-entry trade should be placed.
 */
public record ReEntryResult(
        boolean allowed,
        String reason
) {
    public static ReEntryResult allowed(String reason) {
        return new ReEntryResult(true, reason);
    }

    public static ReEntryResult blocked(String reason) {
        return new ReEntryResult(false, reason);
    }
}
