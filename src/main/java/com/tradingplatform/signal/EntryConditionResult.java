package com.tradingplatform.signal;

import com.tradingplatform.domain.enums.OptionType;

import java.math.BigDecimal;

/**
 * Result of checking all BRD entry conditions for a potential trade.
 * Carries the full picture so rejected signals can be audited
 * ("why didn't it trade at 10:15?") from the dashboard.
 *
 * @param passed        true only when ALL conditions pass
 * @param optionType    CE or PE — which side triggered
 * @param rejectionReason  null if passed, otherwise the first failing condition
 * @param premium       option LTP at time of check
 * @param rsi           RSI value at time of check
 * @param volumeRatio   currentVolume / previousCandleVolume
 * @param delta         option delta at time of check
 */
public record EntryConditionResult(
        boolean passed,
        OptionType optionType,
        String rejectionReason,
        BigDecimal premium,
        BigDecimal rsi,
        BigDecimal volumeRatio,
        BigDecimal delta
) {
    public static EntryConditionResult rejected(OptionType optionType, String reason,
                                                 BigDecimal premium, BigDecimal rsi,
                                                 BigDecimal volumeRatio, BigDecimal delta) {
        return new EntryConditionResult(false, optionType, reason, premium, rsi, volumeRatio, delta);
    }

    public static EntryConditionResult passed(OptionType optionType,
                                               BigDecimal premium, BigDecimal rsi,
                                               BigDecimal volumeRatio, BigDecimal delta) {
        return new EntryConditionResult(true, optionType, null, premium, rsi, volumeRatio, delta);
    }
}
