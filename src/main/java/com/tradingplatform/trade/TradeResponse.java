package com.tradingplatform.trade;

import com.tradingplatform.domain.Trade;
import com.tradingplatform.domain.enums.*;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(
        boolean executed,
        String reason,
        Long tradeId,
        String brokerOrderId,
        IndexName indexName,
        String tradingSymbol,
        TransactionType transactionType,
        Integer quantity,
        BigDecimal entryPrice,
        BigDecimal stopLossPrice,
        BigDecimal target1Price,
        BigDecimal target2Price,
        TradeStatus status,
        boolean reentry,
        Instant entryTime
) {
    public static TradeResponse from(TradeExecutionResult result) {
        if (!result.executed() || result.trade() == null) {
            return new TradeResponse(false, result.reason(),
                    null, null, null, null, null, null,
                    null, null, null, null, null, false, null);
        }
        Trade t = result.trade();
        return new TradeResponse(
                true, result.reason(),
                t.getId(), t.getBrokerOrderId(),
                t.getIndexName(), t.getTradingSymbol(),
                t.getTransactionType(), t.getQuantity(),
                t.getEntryPrice(), t.getStopLossPrice(),
                t.getTarget1Price(), t.getTarget2Price(),
                t.getStatus(), t.isReentry(), t.getEntryTime()
        );
    }
}
