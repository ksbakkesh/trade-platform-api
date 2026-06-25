package com.tradingplatform.strategy.gann;

import com.tradingplatform.domain.enums.IndexName;

import java.math.BigDecimal;

/**
 * Fixed formula constants per index, straight from BRD Sections 4 (NIFTY) and 5 (SENSEX).
 *
 * These are NOT admin-configurable (unlike StrategySettings' premium/RSI/SL/target
 * thresholds) - they're the structural coefficients of the Gann square-root formula
 * itself, tied to each index's typical point range. Changing them would mean
 * redefining the strategy, not tuning it.
 */
public enum GannConstants {

    NIFTY(
            IndexName.NIFTY,
            new BigDecimal("0.01562"),   // offset added/subtracted from sqrt(open) for Buy Above / Sell Below
            new BigDecimal("200"),       // CE strike adjustment
            new BigDecimal("200"),       // PE strike adjustment
            new BigDecimal("60")         // points subtracted from open price for spot stop loss
    ),
    SENSEX(
            IndexName.SENSEX,
            new BigDecimal("0.3124"),
            new BigDecimal("500"),
            new BigDecimal("500"),
            new BigDecimal("60")
    );

    private final IndexName indexName;
    private final BigDecimal offset;
    private final BigDecimal ceStrikeAdjustment;
    private final BigDecimal peStrikeAdjustment;
    private final BigDecimal spotStopLossPoints;

    GannConstants(IndexName indexName, BigDecimal offset, BigDecimal ceStrikeAdjustment,
                  BigDecimal peStrikeAdjustment, BigDecimal spotStopLossPoints) {
        this.indexName = indexName;
        this.offset = offset;
        this.ceStrikeAdjustment = ceStrikeAdjustment;
        this.peStrikeAdjustment = peStrikeAdjustment;
        this.spotStopLossPoints = spotStopLossPoints;
    }

    public static GannConstants forIndex(IndexName indexName) {
        for (GannConstants constants : values()) {
            if (constants.indexName == indexName) {
                return constants;
            }
        }
        throw new IllegalArgumentException("No Gann constants defined for index: " + indexName);
    }

    public IndexName getIndexName() {
        return indexName;
    }

    public BigDecimal getOffset() {
        return offset;
    }

    public BigDecimal getCeStrikeAdjustment() {
        return ceStrikeAdjustment;
    }

    public BigDecimal getPeStrikeAdjustment() {
        return peStrikeAdjustment;
    }

    public BigDecimal getSpotStopLossPoints() {
        return spotStopLossPoints;
    }
}
