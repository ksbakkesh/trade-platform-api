package com.tradingplatform.position;

import com.tradingplatform.domain.enums.IndexName;

/**
 * Exchange-mandated lot sizes for each index's options contracts.
 *
 * IMPORTANT: SEBI/NSE/BSE periodically revise lot sizes (usually on expiry
 * rollover). When that happens, update these values AND add a comment with
 * the effective date so there's an audit trail of which lot size applied when.
 *
 * Current values (effective as of 2024):
 *   NIFTY  = 75  (revised from 50 in Nov 2024)
 *   SENSEX = 20
 */
public enum IndexLotSize {

    NIFTY(IndexName.NIFTY, 75),
    SENSEX(IndexName.SENSEX, 20);

    private final IndexName indexName;
    private final int lotSize;

    IndexLotSize(IndexName indexName, int lotSize) {
        this.indexName = indexName;
        this.lotSize = lotSize;
    }

    public static int forIndex(IndexName indexName) {
        for (IndexLotSize entry : values()) {
            if (entry.indexName == indexName) {
                return entry.lotSize;
            }
        }
        throw new IllegalArgumentException("No lot size defined for index: " + indexName);
    }

    public int getLotSize() {
        return lotSize;
    }
}
