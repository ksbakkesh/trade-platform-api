package com.tradingplatform.strategy.gann;

import com.tradingplatform.domain.enums.IndexName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GannCalculationServiceTest {

    private final GannCalculationService service = new GannCalculationService();

    @Test
    void calculatesNiftyLevelsCorrectly() {
        // Cross-checked against an independent Python/Decimal reference implementation.
        GannLevels levels = service.calculate(IndexName.NIFTY, new BigDecimal("24350.50"));

        assertEquals(new BigDecimal("24355.38"), levels.buyAbove());
        assertEquals(new BigDecimal("24345.63"), levels.sellBelow());
        assertEquals(new BigDecimal("24290.50"), levels.spotStopLoss());
        assertEquals(new BigDecimal("24500.00"), levels.ceStrike());
        assertEquals(new BigDecimal("24200.00"), levels.peStrike());
    }

    @Test
    void calculatesSensexLevelsCorrectly() {
        GannLevels levels = service.calculate(IndexName.SENSEX, new BigDecimal("80125.75"));

        assertEquals(new BigDecimal("80302.71"), levels.buyAbove());
        assertEquals(new BigDecimal("79948.99"), levels.sellBelow());
        assertEquals(new BigDecimal("80065.75"), levels.spotStopLoss());
        assertEquals(new BigDecimal("80700.00"), levels.ceStrike());
        assertEquals(new BigDecimal("79500.00"), levels.peStrike());
    }

    @Test
    void roundsHalfAwayFromZeroAtHundredBoundary() {
        assertEquals(new BigDecimal("24200"), GannRounding.roundToNearestHundred(new BigDecimal("24150")));
        assertEquals(new BigDecimal("24100"), GannRounding.roundToNearestHundred(new BigDecimal("24149.99")));
        assertEquals(new BigDecimal("24200"), GannRounding.roundToNearestHundred(new BigDecimal("24150.01")));
        assertEquals(new BigDecimal("-24200"), GannRounding.roundToNearestHundred(new BigDecimal("-24150")));
    }

    @Test
    void rejectsZeroOrNegativeOpenPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(IndexName.NIFTY, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(IndexName.NIFTY, new BigDecimal("-100")));
    }

    @Test
    void rejectsNullOpenPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(IndexName.NIFTY, null));
    }
}
