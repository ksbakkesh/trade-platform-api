package com.tradingplatform.signal;

import com.tradingplatform.domain.Signal;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.domain.enums.OptionType;
import com.tradingplatform.domain.enums.SignalStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Safe response object for signal endpoints.
 * Never returns the JPA entity directly — lazy proxies (BrokerAccount etc.)
 * can't be serialized outside a transaction.
 */
public record SignalResponse(
        Long id,
        IndexName indexName,
        OptionType signalType,
        BigDecimal openPrice,
        BigDecimal buyAbove,
        BigDecimal sellBelow,
        BigDecimal strikePrice,
        String tradingSymbol,
        String symbolToken,
        BigDecimal premiumAtSignal,
        BigDecimal rsiValue,
        BigDecimal volumeRatio,
        BigDecimal deltaValue,
        SignalStatus status,
        String rejectionReason,
        Instant generatedAt
) {
    public static SignalResponse from(Signal signal) {
        return new SignalResponse(
                signal.getId(),
                signal.getIndexName(),
                signal.getSignalType(),
                signal.getOpenPrice(),
                signal.getBuyAbove(),
                signal.getSellBelow(),
                signal.getStrikePrice(),
                signal.getTradingSymbol(),
                signal.getSymbolToken(),
                signal.getPremiumAtSignal(),
                signal.getRsiValue(),
                signal.getVolumeRatio(),
                signal.getDeltaValue(),
                signal.getStatus(),
                signal.getRejectionReason(),
                signal.getGeneratedAt()
        );
    }
}
