package com.tradingplatform.signal;

import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.Signal;
import com.tradingplatform.domain.StrategySettings;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.domain.enums.OptionType;
import com.tradingplatform.domain.enums.SignalStatus;
import com.tradingplatform.repository.SignalRepository;
import com.tradingplatform.repository.StrategySettingsRepository;
import com.tradingplatform.strategy.gann.GannCalculationService;
import com.tradingplatform.strategy.gann.GannLevels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Orchestrates the full signal generation flow (BRD Sections 3–5):
 *
 *   1. Look up strategy settings for this account + index
 *   2. Determine open price (AUTO fetched or MANUAL from settings)
 *   3. Run Gann calculation → Buy Above / Sell Below / CE Strike / PE Strike
 *   4. Determine signal direction: if spot > Buy Above → CE, if spot < Sell Below → PE
 *   5. Check all entry conditions (premium, RSI, volume, delta)
 *   6. Persist a Signal record — whether it passed or was rejected
 *
 * Returns the persisted Signal so callers (scheduler, test controller) can
 * inspect the result and decide whether to proceed to trade execution.
 *
 * NOTE: This service is intentionally decoupled from trade execution.
 * The trade execution orchestrator (Section 6) calls this, then separately
 * calls the risk module and position sizer before placing any order.
 */
@Service
public class SignalGenerationService {

    private static final Logger log = LoggerFactory.getLogger(SignalGenerationService.class);

    private final StrategySettingsRepository strategySettingsRepository;
    private final GannCalculationService gannCalculationService;
    private final EntryConditionChecker entryConditionChecker;
    private final SignalRepository signalRepository;

    public SignalGenerationService(StrategySettingsRepository strategySettingsRepository,
                                    GannCalculationService gannCalculationService,
                                    EntryConditionChecker entryConditionChecker,
                                    SignalRepository signalRepository) {
        this.strategySettingsRepository = strategySettingsRepository;
        this.gannCalculationService = gannCalculationService;
        this.entryConditionChecker = entryConditionChecker;
        this.signalRepository = signalRepository;
    }

    /**
     * Generates (and persists) a signal for the given account and index.
     *
     * @param brokerAccount  the account whose strategy settings to use
     * @param indexName      NIFTY or SENSEX
     * @param spotPrice      current spot price of the index (used for direction check)
     * @param snapshot       live market data for the option (premium, RSI, volume, delta)
     * @param tradingSymbol  the option's trading symbol (e.g. "NIFTY28NOV2421500CE")
     * @param symbolToken    Angel One's numeric instrument token for the option
     */
    @Transactional
    public Signal generate(BrokerAccount brokerAccount,
                            IndexName indexName,
                            BigDecimal openPrice,
                            BigDecimal spotPrice,
                            MarketSnapshot snapshot,
                            String tradingSymbol,
                            String symbolToken) {

        StrategySettings settings = strategySettingsRepository
                .findByBrokerAccountIdAndIndexName(brokerAccount.getId(), indexName)
                .orElseThrow(() -> new IllegalStateException(
                        "No strategy settings found for account " + brokerAccount.getId()
                                + " and index " + indexName));

        // Step 1: resolve open price (MANUAL override or the passed-in live open)
        BigDecimal resolvedOpenPrice = resolveOpenPrice(settings, openPrice);

        // Step 2: Gann calculation using the open price
        GannLevels levels = gannCalculationService.calculate(indexName, resolvedOpenPrice);

        log.debug("[{}] Open={} BuyAbove={} SellBelow={} CE={} PE={}",
                indexName, resolvedOpenPrice, levels.buyAbove(), levels.sellBelow(),
                levels.ceStrike(), levels.peStrike());

        // Step 3: determine direction
        // Spot > Buy Above → look for CE entry; Spot < Sell Below → look for PE entry
        OptionType direction = resolveDirection(spotPrice, levels);
        if (direction == null) {
            log.info("[{}] Spot {} between BuyAbove {} and SellBelow {} — no signal direction",
                    indexName, spotPrice, levels.buyAbove(), levels.sellBelow());
            // No directional bias — return a transient (unsaved) signal for the caller to inspect.
            // Not worth persisting since it carries no actionable information.
            Signal noDirection = new Signal();
            noDirection.setBrokerAccount(brokerAccount);
            noDirection.setIndexName(indexName);
            noDirection.setOpenPrice(resolvedOpenPrice);
            noDirection.setBuyAbove(levels.buyAbove());
            noDirection.setSellBelow(levels.sellBelow());
            noDirection.setStrikePrice(BigDecimal.ZERO);
            noDirection.setStatus(SignalStatus.REJECTED);
            noDirection.setRejectionReason("Spot price between Buy Above and Sell Below — no directional bias");
            noDirection.setGeneratedAt(Instant.now());
            return noDirection;
        }

        BigDecimal strikePrice = direction == OptionType.CE ? levels.ceStrike() : levels.peStrike();

        // Step 4: entry condition checks
        EntryConditionResult conditionResult = entryConditionChecker.check(direction, snapshot, settings);

        if (!conditionResult.passed()) {
            log.info("[{}] {} signal rejected: {}", indexName, direction, conditionResult.rejectionReason());
            return persistSignal(brokerAccount, indexName, direction, resolvedOpenPrice, levels,
                    strikePrice, tradingSymbol, symbolToken, snapshot,
                    SignalStatus.REJECTED, conditionResult.rejectionReason());
        }

        log.info("[{}] {} signal GENERATED — strike={} premium={} RSI={} vol_ratio={} delta={}",
                indexName, direction, strikePrice,
                snapshot.ltp(), snapshot.rsi(), conditionResult.volumeRatio(), snapshot.delta());

        return persistSignal(brokerAccount, indexName, direction, resolvedOpenPrice, levels,
                strikePrice, tradingSymbol, symbolToken, snapshot,
                SignalStatus.GENERATED, null);
    }

    /**
     * Returns MANUAL open price if configured, otherwise uses the spot price
     * fetched from Angel One (Section 3 — Open Price Management).
     */
    private BigDecimal resolveOpenPrice(StrategySettings settings, BigDecimal liveSpotPrice) {
        return switch (settings.getOpenPriceMode()) {
            case MANUAL -> {
                if (settings.getManualOpenPrice() == null) {
                    throw new IllegalStateException(
                            "Open price mode is MANUAL but no manual open price is set in strategy settings");
                }
                yield settings.getManualOpenPrice();
            }
            case AUTO -> liveSpotPrice;
        };
    }

    /**
     * CE signal if spot crossed above Buy Above level.
     * PE signal if spot crossed below Sell Below level.
     * null if spot is between the two levels (no trade).
     */
    private OptionType resolveDirection(BigDecimal spotPrice, GannLevels levels) {
        if (spotPrice.compareTo(levels.buyAbove()) > 0) {
            return OptionType.CE;
        }
        if (spotPrice.compareTo(levels.sellBelow()) < 0) {
            return OptionType.PE;
        }
        return null;
    }

    private Signal persistSignal(BrokerAccount brokerAccount,
                                  IndexName indexName,
                                  OptionType direction,
                                  BigDecimal openPrice,
                                  GannLevels levels,
                                  BigDecimal strikePrice,
                                  String tradingSymbol,
                                  String symbolToken,
                                  MarketSnapshot snapshot,
                                  SignalStatus status,
                                  String rejectionReason) {
        Signal signal = new Signal();
        signal.setBrokerAccount(brokerAccount);
        signal.setIndexName(indexName);
        signal.setSignalType(direction);
        signal.setOpenPrice(openPrice);
        signal.setBuyAbove(levels.buyAbove());
        signal.setSellBelow(levels.sellBelow());
        signal.setStrikePrice(strikePrice != null ? strikePrice : BigDecimal.ZERO);
        signal.setTradingSymbol(tradingSymbol);
        signal.setSymbolToken(symbolToken);
        signal.setPremiumAtSignal(snapshot != null ? snapshot.ltp() : null);
        signal.setRsiValue(snapshot != null ? snapshot.rsi() : null);
        signal.setDeltaValue(snapshot != null && snapshot.delta() != null
                ? snapshot.delta().abs() : null);
        signal.setStatus(status);
        signal.setRejectionReason(rejectionReason);
        signal.setGeneratedAt(Instant.now());

        if (snapshot != null && snapshot.currentVolume() != null && snapshot.previousVolume() != null
                && snapshot.previousVolume() > 0) {
            signal.setVolumeRatio(
                    java.math.BigDecimal.valueOf(snapshot.currentVolume())
                            .divide(java.math.BigDecimal.valueOf(snapshot.previousVolume()),
                                    3, java.math.RoundingMode.HALF_UP)
            );
        }

        return signalRepository.save(signal);
    }
}
