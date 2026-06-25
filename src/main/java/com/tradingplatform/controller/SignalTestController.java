package com.tradingplatform.controller;

import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.repository.BrokerAccountRepository;
import com.tradingplatform.signal.MarketSnapshot;
import com.tradingplatform.signal.SignalGenerationService;
import com.tradingplatform.signal.SignalResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Manual test endpoint for signal generation.
 *
 * Lets you simulate a full signal evaluation by passing market data as query
 * params — no live market or WebSocket feed needed for testing.
 *
 * Prerequisite: a strategy_settings row must exist for the account + index.
 * Insert one via psql (adjust values as needed):
 *
 *   INSERT INTO strategy_settings(
 *     broker_account_id, index_name, open_price_mode,
 *     premium_threshold, candle_timeframe_minutes,
 *     rsi_threshold, volume_multiplier, delta_min, delta_max,
 *     stop_loss_points, target1_points, target2_points,
 *     quantity_mode, auto_trading_enabled)
 *   VALUES (1,'NIFTY','AUTO',125,15,60,2,0.45,0.65,100,160,200,'CAPITAL_BASED',false);
 *
 * Example — all conditions pass, CE signal:
 *   curl "http://localhost:8080/api/test/signal/generate?accountId=1&index=NIFTY
 *         &spotPrice=24360&premium=150&rsi=65&currentVolume=20000&prevVolume=8000&delta=0.55
 *         &symbol=NIFTY28NOV2424500CE&token=12345"
 *
 * Example — premium too low, signal rejected:
 *   curl "http://localhost:8080/api/test/signal/generate?accountId=1&index=NIFTY
 *         &spotPrice=24360&premium=100&rsi=65&currentVolume=20000&prevVolume=8000&delta=0.55
 *         &symbol=NIFTY28NOV2424500CE&token=12345"
 */
@RestController
public class SignalTestController {

    private final SignalGenerationService signalGenerationService;
    private final BrokerAccountRepository brokerAccountRepository;

    public SignalTestController(SignalGenerationService signalGenerationService,
                                 BrokerAccountRepository brokerAccountRepository) {
        this.signalGenerationService = signalGenerationService;
        this.brokerAccountRepository = brokerAccountRepository;
    }

    @GetMapping("/api/test/signal/generate")
    public SignalResponse generate(
            @RequestParam Long accountId,
            @RequestParam IndexName index,
            @RequestParam BigDecimal openPrice,
            @RequestParam BigDecimal spotPrice,
            @RequestParam BigDecimal premium,
            @RequestParam BigDecimal rsi,
            @RequestParam Long currentVolume,
            @RequestParam Long prevVolume,
            @RequestParam BigDecimal delta,
            @RequestParam(defaultValue = "TEST_SYMBOL") String symbol,
            @RequestParam(defaultValue = "0") String token) {

        BrokerAccount account = brokerAccountRepository.getReferenceById(accountId);
        MarketSnapshot snapshot = new MarketSnapshot(premium, currentVolume, prevVolume, rsi, delta);
        return SignalResponse.from(
                signalGenerationService.generate(account, index, openPrice, spotPrice, snapshot, symbol, token));
    }
}
