package com.tradingplatform.api;

import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.StrategySettings;
import com.tradingplatform.domain.enums.ExitStrategyMode;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.domain.enums.OpenPriceMode;
import com.tradingplatform.domain.enums.QuantityMode;
import com.tradingplatform.repository.BrokerAccountRepository;
import com.tradingplatform.repository.StrategySettingsRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * BRD Section 12 — Admin configuration for strategy settings.
 * One row per (broker_account, index) — NIFTY and SENSEX configured separately.
 */
@RestController
@RequestMapping("/api/admin/strategy-settings")
public class StrategySettingsController {

    private final StrategySettingsRepository strategySettingsRepository;
    private final BrokerAccountRepository brokerAccountRepository;

    public StrategySettingsController(StrategySettingsRepository strategySettingsRepository,
                                       BrokerAccountRepository brokerAccountRepository) {
        this.strategySettingsRepository = strategySettingsRepository;
        this.brokerAccountRepository = brokerAccountRepository;
    }

    @GetMapping
    public List<StrategySettingsResponse> getAll() {
        return strategySettingsRepository.findAll()
                .stream().map(StrategySettingsResponse::from).toList();
    }

    @GetMapping("/{id}")
    public StrategySettingsResponse getById(@PathVariable Long id) {
        return StrategySettingsResponse.from(findById(id));
    }

    @GetMapping("/account/{accountId}/index/{index}")
    public StrategySettingsResponse getByAccountAndIndex(@PathVariable Long accountId,
                                                          @PathVariable IndexName index) {
        return strategySettingsRepository.findByBrokerAccountIdAndIndexName(accountId, index)
                .map(StrategySettingsResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No strategy settings for account " + accountId + " / " + index));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StrategySettingsResponse create(@Valid @RequestBody StrategySettingsRequest req) {
        BrokerAccount account = brokerAccountRepository.getReferenceById(req.brokerAccountId());
        StrategySettings settings = req.toEntity(account);
        return StrategySettingsResponse.from(strategySettingsRepository.save(settings));
    }

    @PutMapping("/{id}")
    public StrategySettingsResponse update(@PathVariable Long id,
                                            @Valid @RequestBody StrategySettingsRequest req) {
        StrategySettings existing = findById(id);
        req.applyTo(existing);
        return StrategySettingsResponse.from(strategySettingsRepository.save(existing));
    }

    @PatchMapping("/{id}/auto-trading")
    public StrategySettingsResponse toggleAutoTrading(@PathVariable Long id,
                                                       @RequestParam boolean enabled) {
        StrategySettings settings = findById(id);
        settings.setAutoTradingEnabled(enabled);
        return StrategySettingsResponse.from(strategySettingsRepository.save(settings));
    }

    @PatchMapping("/{id}/open-price")
    public StrategySettingsResponse setManualOpenPrice(@PathVariable Long id,
                                                        @RequestParam OpenPriceMode mode,
                                                        @RequestParam(required = false) BigDecimal price) {
        StrategySettings settings = findById(id);
        settings.setOpenPriceMode(mode);
        if (mode == OpenPriceMode.MANUAL && price == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "price is required when mode=MANUAL");
        }
        settings.setManualOpenPrice(price);
        return StrategySettingsResponse.from(strategySettingsRepository.save(settings));
    }

    private StrategySettings findById(Long id) {
        return strategySettingsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Strategy settings not found: " + id));
    }

    // -------------------------------------------------------------------------
    // Request / Response DTOs
    // -------------------------------------------------------------------------

    public record StrategySettingsRequest(
            @NotNull Long brokerAccountId,
            @NotNull IndexName indexName,
            OpenPriceMode openPriceMode,
            BigDecimal manualOpenPrice,
            @NotNull @DecimalMin("0") BigDecimal premiumThreshold,
            Integer candleTimeframeMinutes,
            @NotNull @DecimalMin("0") BigDecimal rsiThreshold,
            @NotNull @DecimalMin("0") BigDecimal volumeMultiplier,
            @NotNull @DecimalMin("0") BigDecimal deltaMin,
            @NotNull @DecimalMin("0") BigDecimal deltaMax,
            @NotNull @DecimalMin("0") BigDecimal stopLossPoints,
            @NotNull @DecimalMin("0") BigDecimal target1Points,
            @NotNull @DecimalMin("0") BigDecimal target2Points,
            ExitStrategyMode exitStrategyMode,
            Boolean reEntryEnabled,
            QuantityMode quantityMode,
            Integer fixedLots,
            Integer fixedQuantity,
            BigDecimal capitalAllocationPercent,
            Integer maxLots,
            Boolean autoTradingEnabled
    ) {
        public StrategySettings toEntity(BrokerAccount account) {
            StrategySettings s = new StrategySettings();
            s.setBrokerAccount(account);
            applyTo(s);
            return s;
        }

        public void applyTo(StrategySettings s) {
            s.setIndexName(indexName);
            s.setOpenPriceMode(openPriceMode != null ? openPriceMode : OpenPriceMode.AUTO);
            s.setManualOpenPrice(manualOpenPrice);
            s.setPremiumThreshold(premiumThreshold);
            s.setCandleTimeframeMinutes(candleTimeframeMinutes != null ? candleTimeframeMinutes : 15);
            s.setRsiThreshold(rsiThreshold);
            s.setVolumeMultiplier(volumeMultiplier);
            s.setDeltaMin(deltaMin);
            s.setDeltaMax(deltaMax);
            s.setStopLossPoints(stopLossPoints);
            s.setTarget1Points(target1Points);
            s.setTarget2Points(target2Points);
            s.setExitStrategyMode(exitStrategyMode != null ? exitStrategyMode : ExitStrategyMode.OPTION1);
            s.setReEntryEnabled(reEntryEnabled != null ? reEntryEnabled : true);
            s.setQuantityMode(quantityMode != null ? quantityMode : QuantityMode.CAPITAL_BASED);
            s.setFixedLots(fixedLots);
            s.setFixedQuantity(fixedQuantity);
            s.setCapitalAllocationPercent(capitalAllocationPercent);
            s.setMaxLots(maxLots);
            s.setAutoTradingEnabled(autoTradingEnabled != null ? autoTradingEnabled : false);
        }
    }

    public record StrategySettingsResponse(
            Long id, Long brokerAccountId, IndexName indexName,
            OpenPriceMode openPriceMode, BigDecimal manualOpenPrice,
            BigDecimal premiumThreshold, Integer candleTimeframeMinutes,
            BigDecimal rsiThreshold, BigDecimal volumeMultiplier,
            BigDecimal deltaMin, BigDecimal deltaMax,
            BigDecimal stopLossPoints, BigDecimal target1Points, BigDecimal target2Points,
            ExitStrategyMode exitStrategyMode, boolean reEntryEnabled,
            QuantityMode quantityMode, Integer fixedLots, Integer fixedQuantity,
            BigDecimal capitalAllocationPercent, Integer maxLots,
            boolean autoTradingEnabled
    ) {
        public static StrategySettingsResponse from(StrategySettings s) {
            return new StrategySettingsResponse(
                    s.getId(), s.getBrokerAccount().getId(), s.getIndexName(),
                    s.getOpenPriceMode(), s.getManualOpenPrice(),
                    s.getPremiumThreshold(), s.getCandleTimeframeMinutes(),
                    s.getRsiThreshold(), s.getVolumeMultiplier(),
                    s.getDeltaMin(), s.getDeltaMax(),
                    s.getStopLossPoints(), s.getTarget1Points(), s.getTarget2Points(),
                    s.getExitStrategyMode(), s.isReEntryEnabled(),
                    s.getQuantityMode(), s.getFixedLots(), s.getFixedQuantity(),
                    s.getCapitalAllocationPercent(), s.getMaxLots(),
                    s.isAutoTradingEnabled()
            );
        }
    }
}
