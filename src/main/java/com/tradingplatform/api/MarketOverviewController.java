package com.tradingplatform.api;

import com.tradingplatform.domain.StrategySettings;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.domain.enums.OpenPriceMode;
import com.tradingplatform.repository.StrategySettingsRepository;
import com.tradingplatform.signal.SignalResponse;
import com.tradingplatform.strategy.gann.GannCalculationService;
import com.tradingplatform.strategy.gann.GannLevels;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/**
 * Dashboard — market overview: today's Gann levels for a given open price.
 * BRD Section 13 "Market Overview" widget.
 */
@RestController
@RequestMapping("/api/dashboard/market")
public class MarketOverviewController {

    private final GannCalculationService gannCalculationService;
    private final StrategySettingsRepository strategySettingsRepository;

    public MarketOverviewController(GannCalculationService gannCalculationService,
                                     StrategySettingsRepository strategySettingsRepository) {
        this.gannCalculationService = gannCalculationService;
        this.strategySettingsRepository = strategySettingsRepository;
    }

    /**
     * Returns today's Gann levels for the given account + index.
     * Uses MANUAL open price from settings if configured, otherwise uses provided liveOpenPrice.
     */
    @GetMapping("/levels")
    public GannLevelsResponse levels(@RequestParam Long accountId,
                                      @RequestParam IndexName index,
                                      @RequestParam(required = false) BigDecimal liveOpenPrice) {
        StrategySettings settings = strategySettingsRepository
                .findByBrokerAccountIdAndIndexName(accountId, index)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No strategy settings for account " + accountId + " / " + index));

        BigDecimal openPrice;
        if (settings.getOpenPriceMode() == OpenPriceMode.MANUAL) {
            if (settings.getManualOpenPrice() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mode is MANUAL but no manual open price is set in strategy settings");
            }
            openPrice = settings.getManualOpenPrice();
        } else {
            if (liveOpenPrice == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "liveOpenPrice is required when open price mode is AUTO");
            }
            openPrice = liveOpenPrice;
        }

        GannLevels levels = gannCalculationService.calculate(index, openPrice);
        return GannLevelsResponse.from(levels, settings.getExitStrategyMode().name());
    }

    public record GannLevelsResponse(
            String indexName,
            BigDecimal openPrice,
            BigDecimal buyAbove,
            BigDecimal sellBelow,
            BigDecimal ceStrike,
            BigDecimal peStrike,
            BigDecimal spotStopLoss,
            String exitStrategyMode
    ) {
        public static GannLevelsResponse from(GannLevels l, String exitMode) {
            return new GannLevelsResponse(
                    l.indexName().name(), l.openPrice(),
                    l.buyAbove(), l.sellBelow(),
                    l.ceStrike(), l.peStrike(),
                    l.spotStopLoss(), exitMode
            );
        }
    }
}
