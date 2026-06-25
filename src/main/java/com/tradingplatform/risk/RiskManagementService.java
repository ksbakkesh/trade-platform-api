package com.tradingplatform.risk;

import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.DailyPnl;
import com.tradingplatform.domain.RiskSettings;
import com.tradingplatform.repository.BrokerAccountRepository;
import com.tradingplatform.repository.DailyPnlRepository;
import com.tradingplatform.repository.RiskSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BRD Section 9 — risk controls:
 *
 *   - Max 2 trades per day (combined NIFTY + SENSEX)
 *   - Max ₹4,500 daily loss (combined NIFTY + SENSEX)
 *   - Trading auto-resumes the next trading day (no manual reset needed)
 *   - Risk budget is per broker_account, not per index
 *
 * Two public operations:
 *   1. checkCanTrade()   — call this BEFORE placing any order
 *   2. recordTradeResult() — call this AFTER a trade closes with its realized P&L
 *
 * Both are @Transactional so the daily_pnl row is always consistent.
 */
@Service
public class RiskManagementService {

    private static final Logger log = LoggerFactory.getLogger(RiskManagementService.class);

    private final RiskSettingsRepository riskSettingsRepository;
    private final DailyPnlRepository dailyPnlRepository;
    private final BrokerAccountRepository brokerAccountRepository;

    public RiskManagementService(RiskSettingsRepository riskSettingsRepository,
                                  DailyPnlRepository dailyPnlRepository,
                                  BrokerAccountRepository brokerAccountRepository) {
        this.riskSettingsRepository = riskSettingsRepository;
        this.dailyPnlRepository = dailyPnlRepository;
        this.brokerAccountRepository = brokerAccountRepository;
    }

    /**
     * Can this broker account place a new trade right now?
     */
    @Transactional
    public RiskCheckResult checkCanTrade(Long brokerAccountId) {
        RiskSettings settings = getRiskSettings(brokerAccountId);
        DailyPnl todayPnl = getOrCreateDailyPnl(brokerAccountId, IndianMarketClock.today());

        int tradesUsed = todayPnl.getTotalTrades();
        BigDecimal lossUsed = todayPnl.getTotalPnl().negate().max(BigDecimal.ZERO);
        int maxTrades = settings.getMaxTradesPerDay();
        BigDecimal lossLimit = settings.getDailyLossLimit();

        if (todayPnl.isTradingDisabled()) {
            log.info("Trade blocked for account {}: trading manually disabled for today", brokerAccountId);
            return blocked("Trading is disabled for today", tradesUsed, maxTrades, lossUsed, lossLimit);
        }

        if (tradesUsed >= maxTrades) {
            log.info("Trade blocked for account {}: max trades reached ({}/{})",
                    brokerAccountId, tradesUsed, maxTrades);
            todayPnl.setMaxTradesHit(true);
            dailyPnlRepository.save(todayPnl);
            return blocked(
                    "Max trades per day reached (" + tradesUsed + "/" + maxTrades + ")",
                    tradesUsed, maxTrades, lossUsed, lossLimit
            );
        }

        if (lossUsed.compareTo(lossLimit) >= 0) {
            log.info("Trade blocked for account {}: daily loss limit hit (₹{} / ₹{})",
                    brokerAccountId, lossUsed, lossLimit);
            todayPnl.setDailyLossLimitHit(true);
            dailyPnlRepository.save(todayPnl);
            return blocked(
                    "Daily loss limit hit (₹" + lossUsed.toPlainString() + " / ₹" + lossLimit.toPlainString() + ")",
                    tradesUsed, maxTrades, lossUsed, lossLimit
            );
        }

        return allowed(tradesUsed, maxTrades, lossUsed, lossLimit);
    }

    @Transactional
    public void recordTradeResult(Long brokerAccountId, BigDecimal realizedPnl) {
        RiskSettings settings = getRiskSettings(brokerAccountId);
        DailyPnl todayPnl = getOrCreateDailyPnl(brokerAccountId, IndianMarketClock.today());

        int newTradeCount = todayPnl.getTotalTrades() + 1;
        BigDecimal newTotalPnl = todayPnl.getTotalPnl().add(realizedPnl);
        BigDecimal lossUsed = newTotalPnl.negate().max(BigDecimal.ZERO);

        todayPnl.setTotalTrades(newTradeCount);
        todayPnl.setTotalPnl(newTotalPnl);

        if (newTradeCount >= settings.getMaxTradesPerDay()) {
            todayPnl.setMaxTradesHit(true);
        }

        if (lossUsed.compareTo(settings.getDailyLossLimit()) >= 0) {
            todayPnl.setDailyLossLimitHit(true);
            log.warn("Daily loss limit hit for account {} after trade: ₹{} loss today",
                    brokerAccountId, lossUsed);
        }

        dailyPnlRepository.save(todayPnl);
        log.info("Recorded trade result for account {}: P&L ₹{}, day total ₹{}, trades today: {}",
                brokerAccountId, realizedPnl, newTotalPnl, newTradeCount);
    }

    @Transactional
    public void incrementTradeCount(Long brokerAccountId) {
        DailyPnl todayPnl = getOrCreateDailyPnl(brokerAccountId, IndianMarketClock.today());
        RiskSettings settings = getRiskSettings(brokerAccountId);

        int newCount = todayPnl.getTotalTrades() + 1;
        todayPnl.setTotalTrades(newCount);

        if (newCount >= settings.getMaxTradesPerDay()) {
            todayPnl.setMaxTradesHit(true);
        }

        dailyPnlRepository.save(todayPnl);
    }

    @Transactional
    public RiskCheckResult getDailyRiskSummary(Long brokerAccountId) {
        RiskSettings settings = getRiskSettings(brokerAccountId);
        DailyPnl todayPnl = getOrCreateDailyPnl(brokerAccountId, IndianMarketClock.today());

        int tradesUsed = todayPnl.getTotalTrades();
        BigDecimal lossUsed = todayPnl.getTotalPnl().negate().max(BigDecimal.ZERO);

        boolean canTrade = !todayPnl.isTradingDisabled()
                && tradesUsed < settings.getMaxTradesPerDay()
                && lossUsed.compareTo(settings.getDailyLossLimit()) < 0;

        return new RiskCheckResult(
                canTrade,
                canTrade ? "Trading allowed" : "Trading blocked",
                tradesUsed,
                settings.getMaxTradesPerDay(),
                lossUsed,
                settings.getDailyLossLimit()
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RiskSettings getRiskSettings(Long brokerAccountId) {
        return riskSettingsRepository.findByBrokerAccountId(brokerAccountId)
                .orElseGet(() -> createDefaultRiskSettings(brokerAccountId));
    }

    @Transactional
    public RiskSettings createDefaultRiskSettings(Long brokerAccountId) {
        log.info("No risk settings found for account {} - creating defaults (max 2 trades, ₹4500 loss limit)",
                brokerAccountId);
        RiskSettings settings = new RiskSettings();
        settings.setBrokerAccount(brokerAccountRepository.getReferenceById(brokerAccountId));
        settings.setMaxTradesPerDay(2);
        settings.setDailyLossLimit(new BigDecimal("4500"));
        settings.setScope("COMBINED");
        return riskSettingsRepository.save(settings);
    }

    private DailyPnl getOrCreateDailyPnl(Long brokerAccountId, LocalDate date) {
        return dailyPnlRepository
                .findByBrokerAccountIdAndTradeDate(brokerAccountId, date)
                .orElseGet(() -> {
                    DailyPnl fresh = new DailyPnl();
                    fresh.setBrokerAccount(brokerAccountRepository.getReferenceById(brokerAccountId));
                    fresh.setTradeDate(date);
                    fresh.setTotalTrades(0);
                    fresh.setTotalPnl(BigDecimal.ZERO);
                    return dailyPnlRepository.save(fresh);
                });
    }

    private RiskCheckResult allowed(int tradesUsed, int maxTrades,
                                     BigDecimal lossUsed, BigDecimal lossLimit) {
        return new RiskCheckResult(true, "Trading allowed", tradesUsed, maxTrades, lossUsed, lossLimit);
    }

    private RiskCheckResult blocked(String reason, int tradesUsed, int maxTrades,
                                     BigDecimal lossUsed, BigDecimal lossLimit) {
        return new RiskCheckResult(false, reason, tradesUsed, maxTrades, lossUsed, lossLimit);
    }
}
