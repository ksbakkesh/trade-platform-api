package com.tradingplatform.position;

import com.tradingplatform.angelone.AngelOneMarketClient;
import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.FundManagement;
import com.tradingplatform.domain.StrategySettings;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.domain.enums.QuantityMode;
import com.tradingplatform.repository.FundManagementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * BRD Section 10 — the three position sizing modes:
 *
 *   Mode 1 (FIXED_LOTS):     quantity = lotSize × fixedLots
 *   Mode 2 (CAPITAL_BASED):  lots = floor((capital × allocation%) / (premium × lotSize))
 *                             quantity = lots × lotSize    [capped at maxLots if set]
 *   Mode 3 (FIXED_QUANTITY): quantity = fixedQuantity (user-entered, no lot-size math)
 *
 * BRD Section 11 — fund validation:
 *   Fetches live funds from Angel One, persists a snapshot in fund_management,
 *   then checks: estimatedCost <= availableMargin. Rejects if insufficient.
 *
 * Call order in trade execution flow:
 *   1. calculate()       → get PositionSize
 *   2. validateFunds()   → confirm margin is available
 *   3. place the order if both pass
 */
@Service
public class PositionSizingService {

    private static final Logger log = LoggerFactory.getLogger(PositionSizingService.class);

    private final AngelOneMarketClient marketClient;
    private final FundManagementRepository fundManagementRepository;

    public PositionSizingService(AngelOneMarketClient marketClient,
                                  FundManagementRepository fundManagementRepository) {
        this.marketClient = marketClient;
        this.fundManagementRepository = fundManagementRepository;
    }

    /**
     * Calculates position size using the mode configured in StrategySettings.
     *
     * @param settings      the strategy config for this index (contains mode + params)
     * @param indexName     NIFTY or SENSEX
     * @param premiumPerUnit current option LTP — used only in CAPITAL_BASED mode
     * @param availableCapital broker account's available funds — used only in CAPITAL_BASED mode
     */
    public PositionSize calculate(StrategySettings settings,
                                   IndexName indexName,
                                   BigDecimal premiumPerUnit,
                                   BigDecimal availableCapital) {
        int lotSize = IndexLotSize.forIndex(indexName);
        QuantityMode mode = settings.getQuantityMode();

        return switch (mode) {
            case FIXED_LOTS -> calculateFixedLots(settings, indexName, lotSize, premiumPerUnit);
            case CAPITAL_BASED -> calculateCapitalBased(settings, indexName, lotSize,
                    premiumPerUnit, availableCapital);
            case FIXED_QUANTITY -> calculateFixedQuantity(settings, indexName, lotSize, premiumPerUnit);
        };
    }

    /**
     * Mode 1 — FIXED_LOTS
     * quantity = lotSize × fixedLots
     */
    private PositionSize calculateFixedLots(StrategySettings settings, IndexName indexName,
                                             int lotSize, BigDecimal premiumPerUnit) {
        int lots = requirePositive(settings.getFixedLots(), "fixedLots", QuantityMode.FIXED_LOTS);
        int quantity = lotSize * lots;
        BigDecimal cost = premiumPerUnit.multiply(BigDecimal.valueOf(quantity));

        log.debug("[FIXED_LOTS] {} → {} lots × {} = {} units, estimated cost ₹{}",
                indexName, lots, lotSize, quantity, cost);

        return new PositionSize(indexName, QuantityMode.FIXED_LOTS, lotSize, lots, quantity,
                premiumPerUnit, cost);
    }

    /**
     * Mode 2 — CAPITAL_BASED (recommended per BRD)
     * From BRD example:
     *   Capital Per Trade  = availableCapital × (allocation% / 100)
     *   Cost Per Lot       = premium × lotSize
     *   Calculated Lots    = floor(Capital Per Trade / Cost Per Lot)
     *   Order Quantity     = Calculated Lots × lotSize
     *   [capped at maxLots if configured]
     */
    private PositionSize calculateCapitalBased(StrategySettings settings, IndexName indexName,
                                                int lotSize, BigDecimal premiumPerUnit,
                                                BigDecimal availableCapital) {
        if (availableCapital == null || availableCapital.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Available capital must be positive for CAPITAL_BASED mode, got: " + availableCapital);
        }
        BigDecimal allocationPercent = requireNonNull(settings.getCapitalAllocationPercent(),
                "capitalAllocationPercent", QuantityMode.CAPITAL_BASED);

        BigDecimal capitalPerTrade = availableCapital
                .multiply(allocationPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal costPerLot = premiumPerUnit.multiply(BigDecimal.valueOf(lotSize));

        if (costPerLot.signum() <= 0) {
            throw new IllegalArgumentException("Premium must be positive, got: " + premiumPerUnit);
        }

        int calculatedLots = capitalPerTrade.divide(costPerLot, 0, RoundingMode.FLOOR).intValue();

        // Cap at maxLots if admin has set one
        if (settings.getMaxLots() != null && settings.getMaxLots() > 0) {
            calculatedLots = Math.min(calculatedLots, settings.getMaxLots());
        }

        int quantity = lotSize * calculatedLots;
        BigDecimal cost = premiumPerUnit.multiply(BigDecimal.valueOf(quantity));

        log.debug("[CAPITAL_BASED] {} → capital ₹{}, {}% = ₹{}/trade, cost/lot ₹{}, {} lots, {} units",
                indexName, availableCapital, allocationPercent, capitalPerTrade,
                costPerLot, calculatedLots, quantity);

        return new PositionSize(indexName, QuantityMode.CAPITAL_BASED, lotSize,
                calculatedLots, quantity, premiumPerUnit, cost);
    }

    /**
     * Mode 3 — FIXED_QUANTITY
     * User enters the exact quantity; no lot-size calculation.
     */
    private PositionSize calculateFixedQuantity(StrategySettings settings, IndexName indexName,
                                                 int lotSize, BigDecimal premiumPerUnit) {
        int quantity = requirePositive(settings.getFixedQuantity(), "fixedQuantity",
                QuantityMode.FIXED_QUANTITY);
        // Lots is informational only in this mode — round up to nearest lot
        int lots = (int) Math.ceil((double) quantity / lotSize);
        BigDecimal cost = premiumPerUnit.multiply(BigDecimal.valueOf(quantity));

        log.debug("[FIXED_QUANTITY] {} → {} units (≈{} lots), estimated cost ₹{}",
                indexName, quantity, lots, cost);

        return new PositionSize(indexName, QuantityMode.FIXED_QUANTITY, lotSize,
                lots, quantity, premiumPerUnit, cost);
    }

    /**
     * BRD Section 11: fund validation.
     * Fetches live funds from Angel One, persists a snapshot, then checks
     * whether the estimated cost fits within available margin.
     *
     * @param brokerAccount the account to validate against
     * @param positionSize  the sizing result to validate
     */
    @Transactional
    public FundValidationResult validateFunds(BrokerAccount brokerAccount, PositionSize positionSize) {
        FundManagement funds = fetchAndPersistFunds(brokerAccount);

        BigDecimal available = funds.getAvailableMargin();
        if (available == null) {
            available = funds.getAvailableFunds();
        }

        if (available == null || available.signum() <= 0) {
            log.warn("Fund validation failed for account {}: could not determine available margin",
                    brokerAccount.getId());
            return FundValidationResult.insufficient(positionSize.estimatedCost(), BigDecimal.ZERO);
        }

        if (positionSize.estimatedCost().compareTo(available) <= 0) {
            log.info("Fund validation passed for account {}: required ₹{}, available ₹{}",
                    brokerAccount.getId(), positionSize.estimatedCost(), available);
            return FundValidationResult.sufficient(positionSize.estimatedCost(), available);
        }

        log.warn("Fund validation FAILED for account {}: required ₹{}, available ₹{}",
                brokerAccount.getId(), positionSize.estimatedCost(), available);
        return FundValidationResult.insufficient(positionSize.estimatedCost(), available);
    }

    /**
     * Fetches live funds from Angel One's RMS endpoint and persists a snapshot
     * in fund_management for historical reporting (BRD Section 13).
     */
    private FundManagement fetchAndPersistFunds(BrokerAccount brokerAccount) {
        var fundsData = marketClient.getFunds();

        BigDecimal availableCash = parseSafely(fundsData.getAvailableCash());
        BigDecimal availableMargin = parseSafely(fundsData.getAvailableIntradayPayin());
        BigDecimal utilisedDebits = parseSafely(fundsData.getUtilisedDebits());
        BigDecimal todayPnl = parseSafely(fundsData.getM2mRealized());

        FundManagement snapshot = new FundManagement();
        snapshot.setBrokerAccount(brokerAccount);
        snapshot.setAvailableFunds(availableCash);
        snapshot.setAvailableMargin(availableMargin);
        snapshot.setUsedMargin(utilisedDebits);
        snapshot.setTodayPnl(todayPnl);
        snapshot.setFetchedAt(Instant.now());

        FundManagement saved = fundManagementRepository.save(snapshot);
        log.info("Fund snapshot for account {}: available cash ₹{}, intraday margin ₹{}",
                brokerAccount.getId(), availableCash, availableMargin);
        return saved;
    }

    private BigDecimal parseSafely(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse fund value '{}' as BigDecimal", value);
            return null;
        }
    }

    private int requirePositive(Integer value, String fieldName, QuantityMode mode) {
        if (value == null || value <= 0) {
            throw new IllegalStateException(
                    "StrategySettings." + fieldName + " must be set and > 0 for mode " + mode
                            + ", got: " + value);
        }
        return value;
    }

    private <T> T requireNonNull(T value, String fieldName, QuantityMode mode) {
        if (value == null) {
            throw new IllegalStateException(
                    "StrategySettings." + fieldName + " must be set for mode " + mode);
        }
        return value;
    }
}
