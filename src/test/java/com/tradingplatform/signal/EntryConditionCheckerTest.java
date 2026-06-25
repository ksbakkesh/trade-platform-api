package com.tradingplatform.signal;

import com.tradingplatform.domain.StrategySettings;
import com.tradingplatform.domain.enums.OptionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EntryConditionCheckerTest {

    private final EntryConditionChecker checker = new EntryConditionChecker();
    private StrategySettings niftySettings;

    @BeforeEach
    void setup() {
        niftySettings = new StrategySettings();
        niftySettings.setPremiumThreshold(new BigDecimal("125"));
        niftySettings.setRsiThreshold(new BigDecimal("60"));
        niftySettings.setVolumeMultiplier(new BigDecimal("2"));
        niftySettings.setDeltaMin(new BigDecimal("0.450"));
        niftySettings.setDeltaMax(new BigDecimal("0.650"));
    }

    @Test
    void passesWhenAllConditionsMet() {
        MarketSnapshot snapshot = new MarketSnapshot(
                new BigDecimal("150"),   // premium > 125 ✓
                20000L,                  // current volume
                8000L,                   // previous volume — ratio = 2.5× ≥ 2× ✓
                new BigDecimal("65"),    // RSI > 60 ✓
                new BigDecimal("0.55")   // delta in [0.45, 0.65] ✓
        );

        EntryConditionResult result = checker.check(OptionType.CE, snapshot, niftySettings);

        assertTrue(result.passed());
        assertNull(result.rejectionReason());
        assertEquals(OptionType.CE, result.optionType());
    }

    @Test
    void rejectsWhenPremiumAtOrBelowThreshold() {
        MarketSnapshot snapshot = new MarketSnapshot(
                new BigDecimal("125"),   // exactly at threshold — must be GREATER THAN
                20000L, 8000L,
                new BigDecimal("65"),
                new BigDecimal("0.55")
        );

        EntryConditionResult result = checker.check(OptionType.CE, snapshot, niftySettings);

        assertFalse(result.passed());
        assertTrue(result.rejectionReason().contains("threshold"));
    }

    @Test
    void rejectsWhenRsiAtOrBelowThreshold() {
        MarketSnapshot snapshot = new MarketSnapshot(
                new BigDecimal("150"),
                20000L, 8000L,
                new BigDecimal("60"),   // exactly at threshold — must be GREATER THAN
                new BigDecimal("0.55")
        );

        EntryConditionResult result = checker.check(OptionType.CE, snapshot, niftySettings);

        assertFalse(result.passed());
        assertTrue(result.rejectionReason().contains("RSI"));
    }

    @Test
    void rejectsWhenVolumeBelowMultiplier() {
        MarketSnapshot snapshot = new MarketSnapshot(
                new BigDecimal("150"),
                15000L,   // 15000 < 2 × 8000 = 16000
                8000L,
                new BigDecimal("65"),
                new BigDecimal("0.55")
        );

        EntryConditionResult result = checker.check(OptionType.CE, snapshot, niftySettings);

        assertFalse(result.passed());
        assertTrue(result.rejectionReason().contains("Volume"));
    }

    @Test
    void passesWhenVolumeExactlyAtMultiplier() {
        MarketSnapshot snapshot = new MarketSnapshot(
                new BigDecimal("150"),
                16000L,   // exactly 2 × 8000 — >= so should PASS
                8000L,
                new BigDecimal("65"),
                new BigDecimal("0.55")
        );

        EntryConditionResult result = checker.check(OptionType.CE, snapshot, niftySettings);

        assertTrue(result.passed());
    }

    @Test
    void rejectsWhenDeltaBelowMin() {
        MarketSnapshot snapshot = new MarketSnapshot(
                new BigDecimal("150"),
                20000L, 8000L,
                new BigDecimal("65"),
                new BigDecimal("0.449")  // below 0.45
        );

        EntryConditionResult result = checker.check(OptionType.CE, snapshot, niftySettings);

        assertFalse(result.passed());
        assertTrue(result.rejectionReason().contains("Delta"));
    }

    @Test
    void rejectsWhenDeltaAboveMax() {
        MarketSnapshot snapshot = new MarketSnapshot(
                new BigDecimal("150"),
                20000L, 8000L,
                new BigDecimal("65"),
                new BigDecimal("0.651")  // above 0.65
        );

        EntryConditionResult result = checker.check(OptionType.CE, snapshot, niftySettings);

        assertFalse(result.passed());
        assertTrue(result.rejectionReason().contains("Delta"));
    }

    @Test
    void passesWhenDeltaAtBoundary() {
        MarketSnapshot snapshotMin = new MarketSnapshot(
                new BigDecimal("150"), 20000L, 8000L,
                new BigDecimal("65"), new BigDecimal("0.450")  // exactly at min
        );
        MarketSnapshot snapshotMax = new MarketSnapshot(
                new BigDecimal("150"), 20000L, 8000L,
                new BigDecimal("65"), new BigDecimal("0.650")  // exactly at max
        );

        assertTrue(checker.check(OptionType.CE, snapshotMin, niftySettings).passed());
        assertTrue(checker.check(OptionType.CE, snapshotMax, niftySettings).passed());
    }

    @Test
    void rejectsWhenNullPremium() {
        MarketSnapshot snapshot = new MarketSnapshot(null, 20000L, 8000L,
                new BigDecimal("65"), new BigDecimal("0.55"));

        assertFalse(checker.check(OptionType.CE, snapshot, niftySettings).passed());
    }

    @Test
    void rejectsWhenZeroPreviousVolume() {
        MarketSnapshot snapshot = new MarketSnapshot(
                new BigDecimal("150"), 20000L, 0L,
                new BigDecimal("65"), new BigDecimal("0.55")
        );

        EntryConditionResult result = checker.check(OptionType.CE, snapshot, niftySettings);

        assertFalse(result.passed());
        assertTrue(result.rejectionReason().contains("Volume"));
    }

    @Test
    void worksSameForPeSignal() {
        MarketSnapshot snapshot = new MarketSnapshot(
                new BigDecimal("400"),   // SENSEX premium threshold
                20000L, 8000L,
                new BigDecimal("65"),
                new BigDecimal("-0.55") // PE delta is negative — abs() applied
        );
        StrategySettings sensexSettings = new StrategySettings();
        sensexSettings.setPremiumThreshold(new BigDecimal("350"));
        sensexSettings.setRsiThreshold(new BigDecimal("60"));
        sensexSettings.setVolumeMultiplier(new BigDecimal("2"));
        sensexSettings.setDeltaMin(new BigDecimal("0.450"));
        sensexSettings.setDeltaMax(new BigDecimal("0.650"));

        EntryConditionResult result = checker.check(OptionType.PE, snapshot, sensexSettings);

        assertTrue(result.passed());
        assertEquals(OptionType.PE, result.optionType());
    }
}
