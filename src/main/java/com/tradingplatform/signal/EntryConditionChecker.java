package com.tradingplatform.signal;

import com.tradingplatform.domain.StrategySettings;
import com.tradingplatform.domain.enums.OptionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Checks all BRD entry conditions (Sections 4 & 5) for a potential trade.
 * Pure function — no I/O, no DB, no broker calls. Takes a snapshot + settings,
 * returns a rich result explaining pass/fail.
 *
 * BRD conditions (same for NIFTY and SENSEX, thresholds differ per StrategySettings):
 *   1. Premium Close  > premiumThreshold        (e.g. >125 for NIFTY, >350 for SENSEX)
 *   2. RSI            > rsiThreshold            (default 60)
 *   3. Volume         >= volumeMultiplier × previousCandleVolume  (default 2×)
 *   4. Delta          in [deltaMin, deltaMax]   (default 0.45–0.65)
 *
 * All four must pass. First failure short-circuits and is recorded as rejectionReason.
 */
@Component
public class EntryConditionChecker {

    public EntryConditionResult check(OptionType optionType,
                                       MarketSnapshot snapshot,
                                       StrategySettings settings) {

        BigDecimal premium = snapshot.ltp();
        BigDecimal rsi = snapshot.rsi();
        BigDecimal delta = snapshot.delta() != null ? snapshot.delta().abs() : null;
        BigDecimal volumeRatio = computeVolumeRatio(snapshot);

        // 1. Premium check
        if (premium == null || premium.compareTo(settings.getPremiumThreshold()) <= 0) {
            return EntryConditionResult.rejected(optionType,
                    "Premium ₹" + premium + " ≤ threshold ₹" + settings.getPremiumThreshold(),
                    premium, rsi, volumeRatio, delta);
        }

        // 2. RSI check
        if (rsi == null || rsi.compareTo(settings.getRsiThreshold()) <= 0) {
            return EntryConditionResult.rejected(optionType,
                    "RSI " + rsi + " ≤ threshold " + settings.getRsiThreshold(),
                    premium, rsi, volumeRatio, delta);
        }

        // 3. Volume check — current candle volume >= multiplier × previous candle volume
        if (!volumePasses(snapshot, settings)) {
            return EntryConditionResult.rejected(optionType,
                    "Volume ratio " + volumeRatio + " < " + settings.getVolumeMultiplier() + "× previous candle",
                    premium, rsi, volumeRatio, delta);
        }

        // 4. Delta check
        if (delta == null
                || delta.compareTo(settings.getDeltaMin()) < 0
                || delta.compareTo(settings.getDeltaMax()) > 0) {
            return EntryConditionResult.rejected(optionType,
                    "Delta " + delta + " outside range ["
                            + settings.getDeltaMin() + ", " + settings.getDeltaMax() + "]",
                    premium, rsi, volumeRatio, delta);
        }

        return EntryConditionResult.passed(optionType, premium, rsi, volumeRatio, delta);
    }

    private boolean volumePasses(MarketSnapshot snapshot, StrategySettings settings) {
        if (snapshot.currentVolume() == null || snapshot.previousVolume() == null
                || snapshot.previousVolume() == 0) {
            return false;
        }
        // required = multiplier × previousVolume (e.g. 2 × 5000 = 10000)
        BigDecimal required = settings.getVolumeMultiplier()
                .multiply(BigDecimal.valueOf(snapshot.previousVolume()));
        return BigDecimal.valueOf(snapshot.currentVolume()).compareTo(required) >= 0;
    }

    private BigDecimal computeVolumeRatio(MarketSnapshot snapshot) {
        if (snapshot.currentVolume() == null || snapshot.previousVolume() == null
                || snapshot.previousVolume() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(snapshot.currentVolume())
                .divide(BigDecimal.valueOf(snapshot.previousVolume()), 3, RoundingMode.HALF_UP);
    }
}
