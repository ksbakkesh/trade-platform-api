package com.tradingplatform.position;

import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.domain.enums.QuantityMode;

import java.math.BigDecimal;

/**
 * The output of position sizing — everything needed to place the order
 * and validate it against available margin.
 *
 * @param indexName        NIFTY or SENSEX
 * @param quantityMode     which of the 3 modes was used
 * @param lotSize          exchange lot size at time of calculation
 * @param numberOfLots     calculated number of lots (floor of available capital / cost per lot)
 * @param totalQuantity    numberOfLots * lotSize — the actual quantity field in the order
 * @param premiumPerUnit   option premium (LTP) used in the calculation
 * @param estimatedCost    totalQuantity * premiumPerUnit — what Angel One will need as margin
 */
public record PositionSize(
        IndexName indexName,
        QuantityMode quantityMode,
        int lotSize,
        int numberOfLots,
        int totalQuantity,
        BigDecimal premiumPerUnit,
        BigDecimal estimatedCost
) {
    public boolean isValid() {
        return numberOfLots > 0 && totalQuantity > 0;
    }
}
