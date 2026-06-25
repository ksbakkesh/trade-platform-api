package com.tradingplatform.controller;

import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.Signal;
import com.tradingplatform.repository.BrokerAccountRepository;
import com.tradingplatform.repository.SignalRepository;
import com.tradingplatform.trade.TradeExecutionService;
import com.tradingplatform.trade.TradeResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Manual test endpoint for trade execution.
 *
 * Prerequisites:
 *   1. A GENERATED signal in the DB (from /api/test/signal/generate)
 *   2. strategy_settings must have auto_trading_enabled=TRUE:
 *      UPDATE strategy_settings SET auto_trading_enabled=true WHERE broker_account_id=1;
 *   3. Valid Angel One session (call /api/test/angelone/login first)
 *
 * ⚠️  This places a REAL order on Angel One. Only use during market hours
 *     with a signal you actually want to trade.
 *
 * Example:
 *   curl -X POST "http://localhost:8080/api/test/trade/execute/5?accountId=1&premium=150&capital=100000"
 *   (where 5 is the signal id from the signal generate endpoint)
 */
@RestController
public class TradeExecutionTestController {

    private final TradeExecutionService tradeExecutionService;
    private final SignalRepository signalRepository;
    private final BrokerAccountRepository brokerAccountRepository;

    public TradeExecutionTestController(TradeExecutionService tradeExecutionService,
                                         SignalRepository signalRepository,
                                         BrokerAccountRepository brokerAccountRepository) {
        this.tradeExecutionService = tradeExecutionService;
        this.signalRepository = signalRepository;
        this.brokerAccountRepository = brokerAccountRepository;
    }

    @PostMapping("/api/test/trade/execute/{signalId}")
    public TradeResponse execute(@PathVariable Long signalId,
                                  @RequestParam Long accountId,
                                  @RequestParam BigDecimal premium,
                                  @RequestParam(defaultValue = "100000") BigDecimal capital,
                                  @RequestParam(defaultValue = "false") boolean reentry,
                                  @RequestParam(defaultValue = "false") boolean skipFundCheck) {

        Signal signal = signalRepository.findById(signalId)
                .orElseThrow(() -> new IllegalArgumentException("No signal found with id: " + signalId));

        BrokerAccount account = brokerAccountRepository.getReferenceById(accountId);

        return TradeResponse.from(
                tradeExecutionService.execute(signal, account, premium, capital, reentry, skipFundCheck));
    }
}
