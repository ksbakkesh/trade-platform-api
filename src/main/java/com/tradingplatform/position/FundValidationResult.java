package com.tradingplatform.position;

import java.math.BigDecimal;

/**
 * BRD Section 11: fund validation result.
 * Required Margin <= Available Margin → allow. Otherwise reject with message.
 */
public record FundValidationResult(
        boolean sufficient,
        String reason,
        BigDecimal requiredMargin,
        BigDecimal availableMargin
) {
    public static FundValidationResult sufficient(BigDecimal required, BigDecimal available) {
        return new FundValidationResult(true, "Sufficient margin available", required, available);
    }

    public static FundValidationResult insufficient(BigDecimal required, BigDecimal available) {
        return new FundValidationResult(
                false,
                "Insufficient margin: required ₹" + required.toPlainString()
                        + " but only ₹" + available.toPlainString() + " available",
                required,
                available
        );
    }
}
