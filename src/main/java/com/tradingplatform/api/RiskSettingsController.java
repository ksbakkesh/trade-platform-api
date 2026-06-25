package com.tradingplatform.api;

import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.RiskSettings;
import com.tradingplatform.repository.BrokerAccountRepository;
import com.tradingplatform.repository.RiskSettingsRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/**
 * Admin API for risk settings — max trades/day and daily loss cap.
 * One row per broker account (combined NIFTY + SENSEX scope per BRD Section 9).
 */
@RestController
@RequestMapping("/api/admin/risk-settings")
public class RiskSettingsController {

    private final RiskSettingsRepository riskSettingsRepository;
    private final BrokerAccountRepository brokerAccountRepository;

    public RiskSettingsController(RiskSettingsRepository riskSettingsRepository,
                                   BrokerAccountRepository brokerAccountRepository) {
        this.riskSettingsRepository = riskSettingsRepository;
        this.brokerAccountRepository = brokerAccountRepository;
    }

    @GetMapping("/account/{accountId}")
    public RiskSettingsResponse getByAccount(@PathVariable Long accountId) {
        return riskSettingsRepository.findByBrokerAccountId(accountId)
                .map(RiskSettingsResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No risk settings for account " + accountId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RiskSettingsResponse create(@Valid @RequestBody RiskSettingsRequest req) {
        BrokerAccount account = brokerAccountRepository.getReferenceById(req.brokerAccountId());
        RiskSettings settings = new RiskSettings();
        settings.setBrokerAccount(account);
        req.applyTo(settings);
        return RiskSettingsResponse.from(riskSettingsRepository.save(settings));
    }

    @PutMapping("/account/{accountId}")
    public RiskSettingsResponse update(@PathVariable Long accountId,
                                        @Valid @RequestBody RiskSettingsRequest req) {
        RiskSettings settings = riskSettingsRepository.findByBrokerAccountId(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No risk settings for account " + accountId));
        req.applyTo(settings);
        return RiskSettingsResponse.from(riskSettingsRepository.save(settings));
    }

    public record RiskSettingsRequest(
            @NotNull Long brokerAccountId,
            @NotNull @Min(1) Integer maxTradesPerDay,
            @NotNull @DecimalMin("0") BigDecimal dailyLossLimit
    ) {
        public void applyTo(RiskSettings s) {
            s.setMaxTradesPerDay(maxTradesPerDay);
            s.setDailyLossLimit(dailyLossLimit);
        }
    }

    public record RiskSettingsResponse(
            Long id, Long brokerAccountId,
            Integer maxTradesPerDay, BigDecimal dailyLossLimit, String scope
    ) {
        public static RiskSettingsResponse from(RiskSettings s) {
            return new RiskSettingsResponse(
                    s.getId(), s.getBrokerAccount().getId(),
                    s.getMaxTradesPerDay(), s.getDailyLossLimit(), s.getScope()
            );
        }
    }
}
