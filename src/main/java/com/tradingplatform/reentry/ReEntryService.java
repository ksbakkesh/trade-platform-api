package com.tradingplatform.reentry;

import com.tradingplatform.domain.*;
import com.tradingplatform.domain.enums.*;
import com.tradingplatform.exit.ExitResult;
import com.tradingplatform.repository.StrategySettingsRepository;
import com.tradingplatform.repository.TradeRepository;
import com.tradingplatform.risk.RiskCheckResult;
import com.tradingplatform.risk.RiskManagementService;
import com.tradingplatform.signal.EntryConditionChecker;
import com.tradingplatform.signal.EntryConditionResult;
import com.tradingplatform.signal.MarketSnapshot;
import com.tradingplatform.signal.SignalGenerationService;
import com.tradingplatform.trade.TradeExecutionResult;
import com.tradingplatform.trade.TradeExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * BRD Section 8 — Re-Entry Rules.
 *
 * "Re-entry is allowed after Stop Loss is hit if all entry conditions
 *  become valid again."
 *
 * Full re-entry checklist:
 *   1. The just-closed trade was stopped out (ExitReason = STOP_LOSS)
 *   2. re_entry_enabled = true in strategy settings
 *   3. Risk allows another trade today (combined NIFTY + SENSEX cap)
 *   4. All BRD entry conditions pass again on the current market snapshot
 *      (premium, RSI, volume, delta)
 *   5. If all pass → generate a new signal + execute, flagged as isReentry=true
 *
 * This is intentionally called by the exit service AFTER recording a stop-loss
 * close, so the caller doesn't need to know about re-entry logic.
 */
@Service
public class ReEntryService {

    private static final Logger log = LoggerFactory.getLogger(ReEntryService.class);

    private final StrategySettingsRepository strategySettingsRepository;
    private final RiskManagementService riskManagementService;
    private final EntryConditionChecker entryConditionChecker;
    private final SignalGenerationService signalGenerationService;
    private final TradeExecutionService tradeExecutionService;
    private final TradeRepository tradeRepository;

    public ReEntryService(StrategySettingsRepository strategySettingsRepository,
                           RiskManagementService riskManagementService,
                           EntryConditionChecker entryConditionChecker,
                           SignalGenerationService signalGenerationService,
                           TradeExecutionService tradeExecutionService,
                           TradeRepository tradeRepository) {
        this.strategySettingsRepository = strategySettingsRepository;
        this.riskManagementService = riskManagementService;
        this.entryConditionChecker = entryConditionChecker;
        this.signalGenerationService = signalGenerationService;
        this.tradeExecutionService = tradeExecutionService;
        this.tradeRepository = tradeRepository;
    }

    /**
     * Evaluates and optionally executes a re-entry after a stop-loss exit.
     * Call this immediately after the exit service closes a trade on stop-loss.
     *
     * @param exitResult      the result of the stop-loss exit
     * @param stoppedTrade    the trade that was just stopped out
     * @param brokerAccount   the account
     * @param openPrice       today's opening price (for Gann recalculation)
     * @param currentSpotPrice current index spot price
     * @param snapshot        current market snapshot for the option
     * @param availableCapital available funds for position sizing
     */
    public ReEntryResult evaluate(ExitResult exitResult,
                                   Trade stoppedTrade,
                                   BrokerAccount brokerAccount,
                                   BigDecimal openPrice,
                                   BigDecimal currentSpotPrice,
                                   MarketSnapshot snapshot,
                                   BigDecimal availableCapital) {

        // 1. Was this actually a stop-loss exit?
        if (exitResult.trigger() != com.tradingplatform.exit.ExitTrigger.STOP_LOSS) {
            return ReEntryResult.blocked("Re-entry only triggered on stop-loss exits");
        }

        IndexName index = stoppedTrade.getIndexName();

        // 2. Is re-entry enabled in strategy settings?
        StrategySettings settings = strategySettingsRepository
                .findByBrokerAccountIdAndIndexName(brokerAccount.getId(), index)
                .orElseThrow(() -> new IllegalStateException(
                        "No strategy settings for account " + brokerAccount.getId() + " / " + index));

        if (!settings.isReEntryEnabled()) {
            log.info("[{}] Re-entry disabled in strategy settings", index);
            return ReEntryResult.blocked("Re-entry is disabled in strategy settings");
        }

        // 3. Does risk allow another trade today?
        RiskCheckResult riskCheck = riskManagementService.checkCanTrade(brokerAccount.getId());
        if (!riskCheck.allowed()) {
            log.info("[{}] Re-entry blocked by risk module: {}", index, riskCheck.reason());
            return ReEntryResult.blocked("Risk check failed for re-entry: " + riskCheck.reason());
        }

        // 4. Do entry conditions pass again?
        OptionType direction = stoppedTrade.getSignal() != null
                ? stoppedTrade.getSignal().getSignalType()
                : null;

        if (direction == null) {
            return ReEntryResult.blocked("Cannot determine re-entry direction — original signal missing");
        }

        EntryConditionResult conditions = entryConditionChecker.check(direction, snapshot, settings);
        if (!conditions.passed()) {
            log.info("[{}] Re-entry conditions not met: {}", index, conditions.rejectionReason());
            return ReEntryResult.blocked("Entry conditions not met for re-entry: "
                    + conditions.rejectionReason());
        }

        // 5. All checks passed — generate new signal and execute
        log.info("[{}] Re-entry conditions met after SL hit on trade {} — placing re-entry order",
                index, stoppedTrade.getId());

        Signal reEntrySignal = signalGenerationService.generate(
                brokerAccount, index, openPrice, currentSpotPrice, snapshot,
                stoppedTrade.getTradingSymbol(), stoppedTrade.getSymbolToken());

        if (reEntrySignal.getStatus() != SignalStatus.GENERATED) {
            return ReEntryResult.blocked("Signal generation rejected re-entry: "
                    + reEntrySignal.getRejectionReason());
        }

        TradeExecutionResult execution = tradeExecutionService.execute(
                reEntrySignal, brokerAccount,
                snapshot.ltp(), availableCapital,
                true,   // isReentry = true
                false); // skipFundCheck = false in production

        if (!execution.executed()) {
            return ReEntryResult.blocked("Re-entry order failed: " + execution.reason());
        }

        log.info("[{}] Re-entry trade placed: orderId={}",
                index, execution.trade().getBrokerOrderId());
        return ReEntryResult.allowed("Re-entry placed successfully — trade id: "
                + execution.trade().getId());
    }
}
