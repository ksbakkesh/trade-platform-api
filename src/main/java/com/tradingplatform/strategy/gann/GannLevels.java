package com.tradingplatform.strategy.gann;

import com.tradingplatform.domain.enums.IndexName;

import java.math.BigDecimal;

/**
 * The full set of levels derived from a single open price, per BRD Sections 4 & 5.
 *
 * @param indexName     NIFTY or SENSEX
 * @param openPrice     the input - either fetched automatically or entered manually (Section 3)
 * @param root          sqrt(openPrice), kept for transparency/debugging
 * @param buyAbove      (root + offset)^2 - unrounded
 * @param sellBelow     (root - offset)^2 - unrounded
 * @param spotStopLoss  openPrice - 60
 * @param ceStrike      ROUND(buyAbove - 100, -2) + index strike adjustment
 * @param peStrike      ROUND(sellBelow + 100, -2) - index strike adjustment
 */
public record GannLevels(
        IndexName indexName,
        BigDecimal openPrice,
        BigDecimal root,
        BigDecimal buyAbove,
        BigDecimal sellBelow,
        BigDecimal spotStopLoss,
        BigDecimal ceStrike,
        BigDecimal peStrike
) {
}
