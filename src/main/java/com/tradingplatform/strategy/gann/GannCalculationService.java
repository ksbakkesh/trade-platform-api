package com.tradingplatform.strategy.gann;

import com.tradingplatform.domain.enums.IndexName;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Implements the Gann square-root level calculations from BRD Sections 4 (NIFTY)
 * and 5 (SENSEX):
 *
 *   Root        = SQRT(Open Price)
 *   Buy Above   = (Root + offset)^2
 *   Sell Below  = (Root - offset)^2
 *   Spot SL     = Open Price - 60
 *   CE Strike   = ROUND(Buy Above - 100, -2) + strikeAdjustment
 *   PE Strike   = ROUND(Sell Below + 100, -2) - strikeAdjustment
 *
 * Pure function, no broker/DB calls - takes an open price, returns levels.
 * Open price itself comes from Section 3 (auto-fetched or manually entered),
 * which is the caller's responsibility, not this engine's.
 *
 * Intermediate math is done at high precision (20 significant digits) and only
 * rounded to 2 decimal places / nearest 100 at the exact points the BRD's
 * formulas specify - rounding earlier than that can shift a result across a
 * 100-point boundary and produce the wrong strike.
 */
@Service
public class GannCalculationService {

    private static final MathContext CALC_PRECISION = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int DISPLAY_SCALE = 2;

    public GannLevels calculate(IndexName indexName, BigDecimal openPrice) {
        if (openPrice == null || openPrice.signum() <= 0) {
            throw new IllegalArgumentException("Open price must be a positive value, got: " + openPrice);
        }

        GannConstants constants = GannConstants.forIndex(indexName);

        BigDecimal root = openPrice.sqrt(CALC_PRECISION);

        BigDecimal buyAboveRaw = square(root.add(constants.getOffset()));
        BigDecimal sellBelowRaw = square(root.subtract(constants.getOffset()));

        BigDecimal spotStopLoss = openPrice.subtract(constants.getSpotStopLossPoints());

        BigDecimal ceStrike = GannRounding.roundToNearestHundred(buyAboveRaw.subtract(ONE_HUNDRED))
                .add(constants.getCeStrikeAdjustment());
        BigDecimal peStrike = GannRounding.roundToNearestHundred(sellBelowRaw.add(ONE_HUNDRED))
                .subtract(constants.getPeStrikeAdjustment());

        return new GannLevels(
                indexName,
                openPrice.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                root.setScale(6, RoundingMode.HALF_UP),
                buyAboveRaw.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                sellBelowRaw.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                spotStopLoss.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                ceStrike.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                peStrike.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal square(BigDecimal value) {
        return value.multiply(value, CALC_PRECISION);
    }
}
