package com.tradingplatform.strategy.gann;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Extracted from GannCalculationService so the rounding behavior itself can be
 * unit tested with exact values, independent of any sqrt() precision effects.
 */
final class GannRounding {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private GannRounding() {
    }

    /**
     * Equivalent to Excel's ROUND(value, -2): rounds to the nearest 100,
     * halves rounding away from zero (e.g. 24150 -> 24200, not 24100).
     */
    static BigDecimal roundToNearestHundred(BigDecimal value) {
        return value.divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
    }
}
