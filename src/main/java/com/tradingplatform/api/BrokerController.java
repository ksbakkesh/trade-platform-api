package com.tradingplatform.api;

import com.tradingplatform.angelone.AngelOneAuthClient;
import com.tradingplatform.common.ErrorMessages;
import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.User;
import com.tradingplatform.domain.StrategySettings;
import com.tradingplatform.domain.RiskSettings;
import com.tradingplatform.repository.BrokerAccountRepository;
import com.tradingplatform.repository.UserRepository;
import com.tradingplatform.repository.StrategySettingsRepository;
import com.tradingplatform.repository.RiskSettingsRepository;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.domain.enums.OpenPriceMode;
import com.tradingplatform.domain.enums.ExitStrategyMode;
import com.tradingplatform.domain.enums.QuantityMode;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/broker")
public class BrokerController {

    private final BrokerAccountRepository brokerAccountRepository;
    private final UserRepository userRepository;
    private final AngelOneAuthClient angelOneAuthClient;
    private final StrategySettingsRepository strategySettingsRepository;
    private final RiskSettingsRepository riskSettingsRepository;

    public BrokerController(BrokerAccountRepository brokerAccountRepository,
                            UserRepository userRepository,
                            AngelOneAuthClient angelOneAuthClient,
                            StrategySettingsRepository strategySettingsRepository,
                            RiskSettingsRepository riskSettingsRepository) {
        this.brokerAccountRepository = brokerAccountRepository;
        this.userRepository = userRepository;
        this.angelOneAuthClient = angelOneAuthClient;
        this.strategySettingsRepository = strategySettingsRepository;
        this.riskSettingsRepository = riskSettingsRepository;
    }

    @GetMapping("/my-account")
    public ResponseEntity<?> myAccount(Authentication auth) {
        if (auth == null) return unauthorized();
        try {
            Long userId = getUserId(auth);
            return brokerAccountRepository.findByUserIdAndBrokerName(userId, "ANGEL_ONE")
                    .map(a -> ResponseEntity.ok(Map.of(
                            "id", a.getId(),
                            "brokerName", a.getBrokerName(),
                            "clientCode", a.getClientCode(),
                            "isActive", a.isActive(),
                            "createdAt", a.getCreatedAt().toString()
                    )))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/connect/angel-one")
    public ResponseEntity<?> connectAngelOne(@RequestBody ConnectRequest req, Authentication auth) {
        if (auth == null) return unauthorized();
        try {
            Long userId = getUserId(auth);
            User user = userRepository.getReferenceById(userId);

            Optional<BrokerAccount> existing = brokerAccountRepository
                    .findByUserIdAndBrokerName(userId, "ANGEL_ONE");

            boolean isNew = existing.isEmpty();
            BrokerAccount account = existing.orElse(new BrokerAccount());
            account.setUser(user);
            account.setBrokerName("ANGEL_ONE");
            account.setClientCode(req.clientCode());
            account.setApiKey(req.apiKey());
            account.setPassword(req.password());
            account.setTotpSecret(req.totpSecret());
            account.setActive(true);
            brokerAccountRepository.save(account);

            // Auto-create default settings for new accounts
            if (isNew) {
                createDefaultSettings(account);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Angel One connected successfully",
                    "clientCode", req.clientCode(),
                    "accountId", account.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test-connection")
    public ResponseEntity<?> testConnection(Authentication auth) {
        if (auth == null) return unauthorized();
        try {
            Long userId = getUserId(auth);
            BrokerAccount account = brokerAccountRepository
                    .findByUserIdAndBrokerName(userId, "ANGEL_ONE")
                    .orElseThrow(() -> new RuntimeException("No Angel One account connected"));
            angelOneAuthClient.loginForAccount(account.getId());
            return ResponseEntity.ok(Map.of(
                    "message", "Connection successful!",
                    "clientCode", account.getClientCode()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnect(Authentication auth) {
        if (auth == null) return unauthorized();
        try {
            Long userId = getUserId(auth);
            brokerAccountRepository.findByUserIdAndBrokerName(userId, "ANGEL_ONE")
                    .ifPresent(brokerAccountRepository::delete);
            return ResponseEntity.ok(Map.of("message", "Broker disconnected"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ErrorMessages.INTERNAL_ERROR));
        }
    }

    // ── Auto-create default settings for new broker account ──

    private void createDefaultSettings(BrokerAccount account) {
        for (IndexName index : List.of(IndexName.NIFTY, IndexName.SENSEX)) {
            boolean exists = strategySettingsRepository
                    .findByBrokerAccountIdAndIndexName(account.getId(), index)
                    .isPresent();
            if (!exists) {
                StrategySettings s = new StrategySettings();
                s.setBrokerAccount(account);
                s.setIndexName(index);
                s.setOpenPriceMode(OpenPriceMode.AUTO);
                s.setPremiumThreshold(new BigDecimal("125"));
                s.setCandleTimeframeMinutes(15);
                s.setRsiThreshold(new BigDecimal("60"));
                s.setVolumeMultiplier(new BigDecimal("2"));
                s.setDeltaMin(new BigDecimal("0.45"));
                s.setDeltaMax(new BigDecimal("0.65"));
                s.setStopLossPoints(new BigDecimal("100"));
                s.setTarget1Points(new BigDecimal("160"));
                s.setTarget2Points(new BigDecimal("200"));
                s.setExitStrategyMode(ExitStrategyMode.OPTION1);
                s.setReEntryEnabled(true);
                s.setQuantityMode(QuantityMode.CAPITAL_BASED);
                s.setCapitalAllocationPercent(new BigDecimal("20"));
                s.setAutoTradingEnabled(true);
                strategySettingsRepository.save(s);
            }
        }

        boolean riskExists = riskSettingsRepository
                .findByBrokerAccountId(account.getId())
                .isPresent();
        if (!riskExists) {
            RiskSettings r = new RiskSettings();
            r.setBrokerAccount(account);
            r.setMaxTradesPerDay(2);
            r.setDailyLossLimit(new BigDecimal("4500"));
            riskSettingsRepository.save(r);
        }
    }

    // ── helpers ──

    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ErrorMessages.NOT_AUTHENTICATED));
    }

    public record ConnectRequest(
            @NotBlank String clientCode,
            @NotBlank String apiKey,
            @NotBlank String password,
            @NotBlank String totpSecret
    ) {}
}