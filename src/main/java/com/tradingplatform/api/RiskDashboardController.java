package com.tradingplatform.api;

import com.tradingplatform.domain.DailyPnl;
import com.tradingplatform.repository.DailyPnlRepository;
import com.tradingplatform.risk.IndianMarketClock;
import com.tradingplatform.risk.RiskCheckResult;
import com.tradingplatform.risk.RiskManagementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Dashboard — risk summary and daily P&L.
 * Backs the BRD Section 13 "Risk Summary" and "Daily P&L" widgets.
 */
@RestController
@RequestMapping("/api/dashboard/risk")
public class RiskDashboardController {

    private final RiskManagementService riskManagementService;
    private final DailyPnlRepository dailyPnlRepository;

    public RiskDashboardController(RiskManagementService riskManagementService,
                                    DailyPnlRepository dailyPnlRepository) {
        this.riskManagementService = riskManagementService;
        this.dailyPnlRepository = dailyPnlRepository;
    }

    /**
     * Live risk status — trades used, loss used, remaining budget.
     * This is the primary widget on the BRD Section 13 dashboard.
     */
    @GetMapping("/summary")
    public RiskSummaryResponse summary(@RequestParam Long accountId) {
        RiskCheckResult check = riskManagementService.getDailyRiskSummary(accountId);
        return new RiskSummaryResponse(
                check.allowed(),
                check.reason(),
                check.tradesUsedToday(),
                check.maxTradesPerDay(),
                check.remainingTrades(),
                check.lossUsedToday(),
                check.dailyLossLimit(),
                check.remainingLossBudget()
        );
    }

    /**
     * Daily P&L for a specific date (defaults to today IST).
     */
    @GetMapping("/daily-pnl")
    public DailyPnlResponse dailyPnl(@RequestParam Long accountId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : IndianMarketClock.today();
        Optional<DailyPnl> pnl = dailyPnlRepository
                .findByBrokerAccountIdAndTradeDate(accountId, targetDate);

        return pnl.map(p -> new DailyPnlResponse(
                p.getTradeDate(),
                p.getTotalTrades(),
                p.getTotalPnl(),
                p.isDailyLossLimitHit(),
                p.isMaxTradesHit(),
                p.isTradingDisabled()
        )).orElse(new DailyPnlResponse(targetDate, 0, BigDecimal.ZERO, false, false, false));
    }

    public record RiskSummaryResponse(
            boolean tradingAllowed,
            String reason,
            int tradesUsedToday,
            int maxTradesPerDay,
            int remainingTrades,
            BigDecimal lossUsedToday,
            BigDecimal dailyLossLimit,
            BigDecimal remainingLossBudget
    ) {}

    public record DailyPnlResponse(
            LocalDate tradeDate,
            int totalTrades,
            BigDecimal totalPnl,
            boolean dailyLossLimitHit,
            boolean maxTradesHit,
            boolean tradingDisabled
    ) {}
}
