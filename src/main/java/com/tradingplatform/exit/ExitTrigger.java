package com.tradingplatform.exit;

/**
 * What price level was hit during an exit check.
 * The exit service evaluates these in priority order:
 * STOP_LOSS > TARGET1 > TARGET2 > NONE
 */
public enum ExitTrigger {
    NONE,        // LTP is between SL and Target 1 — hold
    STOP_LOSS,   // LTP <= stop loss price
    TARGET1,     // LTP >= target 1 price
    TARGET2      // LTP >= target 2 price (only relevant after T1 hit)
}
