package com.tradingplatform.api;

import com.tradingplatform.domain.Trade;
import com.tradingplatform.domain.enums.*;
import com.tradingplatform.repository.TradeRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

/**
 * Dashboard — open trades and trade history.
 */
@RestController
@RequestMapping("/api/dashboard/trades")
public class TradeDashboardController {

    private final TradeRepository tradeRepository;

    public TradeDashboardController(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    /** All currently open/partially-closed trades */
    @GetMapping("/open")
    public List<TradeSummary> openTrades(@RequestParam Long accountId) {
        List<TradeSummary> result = new java.util.ArrayList<>();
        tradeRepository.findByBrokerAccountIdAndStatus(accountId, TradeStatus.OPEN)
                .forEach(t -> result.add(TradeSummary.from(t)));
        tradeRepository.findByBrokerAccountIdAndStatus(accountId, TradeStatus.PARTIALLY_CLOSED)
                .forEach(t -> result.add(TradeSummary.from(t)));
        return result;
    }

    /** Today's trades */
    @GetMapping("/today")
    public List<TradeSummary> today(@RequestParam Long accountId) {
        Instant startOfDay = LocalDate.now(ZoneId.of("Asia/Kolkata"))
                .atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        return tradeRepository
                .findByBrokerAccountIdAndEntryTimeBetween(accountId, startOfDay, Instant.now())
                .stream().map(TradeSummary::from).toList();
    }

    /** Trade count today — used by risk dashboard */
    @GetMapping("/today/count")
    public long todayCount(@RequestParam Long accountId) {
        Instant startOfDay = LocalDate.now(ZoneId.of("Asia/Kolkata"))
                .atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        return tradeRepository.countByBrokerAccountIdAndEntryTimeBetween(
                accountId, startOfDay, Instant.now());
    }

    public record TradeSummary(
            Long id, IndexName indexName, String tradingSymbol,
            TransactionType transactionType, Integer quantity,
            BigDecimal entryPrice, BigDecimal stopLossPrice,
            BigDecimal target1Price, BigDecimal target2Price, BigDecimal exitPrice,
            String brokerOrderId, TradeStatus status, ExitReason exitReason,
            BigDecimal realizedPnl, boolean reentry,
            Instant entryTime, Instant exitTime
    ) {
        public static TradeSummary from(Trade t) {
            return new TradeSummary(
                    t.getId(), t.getIndexName(), t.getTradingSymbol(),
                    t.getTransactionType(), t.getQuantity(),
                    t.getEntryPrice(), t.getStopLossPrice(),
                    t.getTarget1Price(), t.getTarget2Price(), t.getExitPrice(),
                    t.getBrokerOrderId(), t.getStatus(), t.getExitReason(),
                    t.getRealizedPnl(), t.isReentry(),
                    t.getEntryTime(), t.getExitTime()
            );
        }
    }
}
