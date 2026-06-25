package com.tradingplatform.signal;

import java.math.BigDecimal;

/**
 * A snapshot of the data needed to evaluate entry conditions for one option.
 * Populated from Angel One quote data + any external RSI/delta source.
 *
 * @param ltp               last traded price (= premium close of the 15-min candle)
 * @param currentVolume     volume of the current (just-closed) 15-min candle
 * @param previousVolume    volume of the previous 15-min candle (for 2× check)
 * @param rsi               RSI(14) of the option or underlying, depending on strategy config
 * @param delta             option delta (0 to 1 for CE, -1 to 0 for PE, passed as absolute)
 */
public record MarketSnapshot(
        BigDecimal ltp,
        Long currentVolume,
        Long previousVolume,
        BigDecimal rsi,
        BigDecimal delta
) {
}
