package com.tradingplatform.controller;

import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.StrategySettings;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.domain.enums.QuantityMode;
import com.tradingplatform.position.FundValidationResult;
import com.tradingplatform.position.PositionSize;
import com.tradingplatform.position.PositionSizingService;
import com.tradingplatform.repository.BrokerAccountRepository;
import com.tradingplatform.repository.StrategySettingsRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Manual verification endpoints for all 3 position sizing modes.
 *
 * Mode 1 — FIXED_LOTS:
 *   curl "http://localhost:8080/api/test/position/size?index=NIFTY&mode=FIXED_LOTS&premium=125&lots=2"
 *
 * Mode 2 — CAPITAL_BASED (BRD example: ₹1,00,000 capital, 20% allocation, ₹125 premium → 2 lots, 150 qty):
 *   curl "http://localhost:8080/api/test/position/size?index=NIFTY&mode=CAPITAL_BASED&premium=125&capital=100000&allocation=20"
 *
 * Mode 3 — FIXED_QUANTITY:
 *   curl "http://localhost:8080/api/test/position/size?index=NIFTY&mode=FIXED_QUANTITY&premium=125&quantity=150"
 *
 * Fund validation (needs a real broker_account row — see RiskTestController for insert SQL):
 *   curl "http://localhost:8080/api/test/position/funds?accountId=1&index=NIFTY&mode=CAPITAL_BASED&premium=125&capital=100000&allocation=20"
 */
@RestController
public class PositionSizingTestController {

    private final PositionSizingService positionSizingService;
    private final BrokerAccountRepository brokerAccountRepository;
    private final StrategySettingsRepository strategySettingsRepository;

    public PositionSizingTestController(PositionSizingService positionSizingService,
                                         BrokerAccountRepository brokerAccountRepository,
                                         StrategySettingsRepository strategySettingsRepository) {
        this.positionSizingService = positionSizingService;
        this.brokerAccountRepository = brokerAccountRepository;
        this.strategySettingsRepository = strategySettingsRepository;
    }

    /**
     * Calculate position size for any mode, without touching the DB or broker.
     * Builds a transient StrategySettings from the query params — no DB row needed.
     */
    @GetMapping("/api/test/position/size")
    public PositionSize size(
            @RequestParam IndexName index,
            @RequestParam QuantityMode mode,
            @RequestParam BigDecimal premium,
            @RequestParam(required = false) Integer lots,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) BigDecimal capital,
            @RequestParam(required = false) BigDecimal allocation,
            @RequestParam(required = false) Integer maxLots) {

        StrategySettings settings = buildSettings(mode, lots, quantity, allocation, maxLots);
        return positionSizingService.calculate(settings, index, premium, capital);
    }

    /**
     * Calculate position size AND validate against live Angel One funds.
     * Requires a real broker_account row in the DB (needs login session active too).
     */
    @GetMapping("/api/test/position/funds")
    public FundValidationResult funds(
            @RequestParam Long accountId,
            @RequestParam IndexName index,
            @RequestParam QuantityMode mode,
            @RequestParam BigDecimal premium,
            @RequestParam(required = false) Integer lots,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) BigDecimal capital,
            @RequestParam(required = false) BigDecimal allocation,
            @RequestParam(required = false) Integer maxLots) {

        BrokerAccount account = brokerAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("No broker account found: " + accountId));

        StrategySettings settings = buildSettings(mode, lots, quantity, allocation, maxLots);
        PositionSize positionSize = positionSizingService.calculate(settings, index, premium, capital);
        return positionSizingService.validateFunds(account, positionSize);
    }

    private StrategySettings buildSettings(QuantityMode mode, Integer lots, Integer quantity,
                                            BigDecimal allocation, Integer maxLots) {
        StrategySettings settings = new StrategySettings();
        settings.setQuantityMode(mode);
        settings.setFixedLots(lots);
        settings.setFixedQuantity(quantity);
        settings.setCapitalAllocationPercent(allocation);
        settings.setMaxLots(maxLots);
        return settings;
    }
}
