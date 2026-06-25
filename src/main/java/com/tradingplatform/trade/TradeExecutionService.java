package com.tradingplatform.trade;

import com.tradingplatform.angelone.AngelOneOrderClient;
import com.tradingplatform.angelone.dto.OrderResponse;
import com.tradingplatform.angelone.dto.PlaceOrderRequest;
import com.tradingplatform.domain.*;
import com.tradingplatform.domain.enums.*;
import com.tradingplatform.position.FundValidationResult;
import com.tradingplatform.position.PositionSize;
import com.tradingplatform.position.PositionSizingService;
import com.tradingplatform.repository.PositionRepository;
import com.tradingplatform.repository.StrategySettingsRepository;
import com.tradingplatform.repository.TradeRepository;
import com.tradingplatform.risk.RiskCheckResult;
import com.tradingplatform.risk.RiskManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * BRD Section 6 — Trade Execution Orchestration.
 *
 * Full flow for every trade attempt:
 *   1. Check risk limits (max trades/day, daily loss cap)
 *   2. Load strategy settings for stop loss / target prices
 *   3. Calculate position size (the 3 BRD modes)
 *   4. Validate available margin (BRD Section 11)
 *   5. Place order with Angel One SmartAPI
 *   6. Record trade count (risk module)
 *   7. Persist Trade entity (full order record)
 *   8. Persist Position entity (live tracking state)
 *
 * This service is deliberately the ONLY place in the codebase that calls
 * AngelOneOrderClient.placeOrder() — all other paths (test controllers,
 * signal generation) cannot place real orders.
 *
 * NOTE: auto_trading_enabled on strategy_settings is checked here.
 * If false, the signal will be generated and logged but no order is placed.
 */
@Service
public class TradeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TradeExecutionService.class);

    private final RiskManagementService riskManagementService;
    private final StrategySettingsRepository strategySettingsRepository;
    private final PositionSizingService positionSizingService;
    private final AngelOneOrderClient orderClient;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;

    public TradeExecutionService(RiskManagementService riskManagementService,
                                  StrategySettingsRepository strategySettingsRepository,
                                  PositionSizingService positionSizingService,
                                  AngelOneOrderClient orderClient,
                                  TradeRepository tradeRepository,
                                  PositionRepository positionRepository) {
        this.riskManagementService = riskManagementService;
        this.strategySettingsRepository = strategySettingsRepository;
        this.positionSizingService = positionSizingService;
        this.orderClient = orderClient;
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
    }

    /**
     * Executes a trade from a generated signal.
     *
     * @param signal          the GENERATED signal to trade on (must have status=GENERATED)
     * @param brokerAccount   the account to trade on
     * @param currentPremium  live option LTP at execution time (may differ from signal premium)
     * @param availableCapital available funds from Angel One RMS (used for CAPITAL_BASED sizing)
     * @param isReentry       true if this is a re-entry after a stop-loss
     */
    @Transactional
    public TradeExecutionResult execute(Signal signal,
                                        BrokerAccount brokerAccount,
                                        BigDecimal currentPremium,
                                        BigDecimal availableCapital,
                                        boolean isReentry,
                                        boolean skipFundCheck) {

        if (signal.getStatus() != SignalStatus.GENERATED) {
            return TradeExecutionResult.blocked(
                    "Signal status is " + signal.getStatus() + " — only GENERATED signals can be traded");
        }

        // Step 1: load strategy settings
        StrategySettings settings = strategySettingsRepository
                .findByBrokerAccountIdAndIndexName(brokerAccount.getId(), signal.getIndexName())
                .orElseThrow(() -> new IllegalStateException(
                        "No strategy settings for account " + brokerAccount.getId()
                                + " / " + signal.getIndexName()));

        // Step 1b: check auto-trading is enabled
        if (!settings.isAutoTradingEnabled()) {
            log.info("[{}] Auto-trading disabled — signal {} logged but no order placed",
                    signal.getIndexName(), signal.getId());
            return TradeExecutionResult.blocked("Auto-trading is disabled in strategy settings");
        }

        // Step 2: risk check
        RiskCheckResult riskCheck = riskManagementService.checkCanTrade(brokerAccount.getId());
        if (!riskCheck.allowed()) {
            log.info("[{}] Trade blocked by risk module: {}", signal.getIndexName(), riskCheck.reason());
            return TradeExecutionResult.blocked("Risk check failed: " + riskCheck.reason());
        }

        // Step 3: position sizing
        PositionSize positionSize = positionSizingService.calculate(
                settings, signal.getIndexName(), currentPremium, availableCapital);

        if (!positionSize.isValid()) {
            log.warn("[{}] Position sizing returned 0 lots — insufficient capital for even 1 lot",
                    signal.getIndexName());
            return TradeExecutionResult.blocked(
                    "Insufficient capital: cannot size even 1 lot at premium ₹" + currentPremium);
        }

        // Step 4: fund validation (skippable for test accounts with no margin)
        if (!skipFundCheck) {
            FundValidationResult fundCheck = positionSizingService.validateFunds(brokerAccount, positionSize);
            if (!fundCheck.sufficient()) {
                log.warn("[{}] Fund validation failed: {}", signal.getIndexName(), fundCheck.reason());
                return TradeExecutionResult.blocked(fundCheck.reason());
            }
        } else {
            log.warn("[{}] Fund check SKIPPED — test mode only, never use in production",
                    signal.getIndexName());
        }

        // Step 5: place order
        PlaceOrderRequest orderRequest = buildOrderRequest(signal, positionSize);
        OrderResponse orderResponse;
        try {
            orderResponse = orderClient.placeOrder(orderRequest);
        } catch (Exception e) {
            log.error("[{}] Order placement failed: {}", signal.getIndexName(), e.getMessage());
            return TradeExecutionResult.failed("Broker order failed: " + e.getMessage());
        }

        // Step 6: record trade count in risk module (before persisting trade,
        // so daily cap is accurate even if DB write fails)
        riskManagementService.incrementTradeCount(brokerAccount.getId());

        // Step 7: persist trade
        Trade trade = persistTrade(signal, brokerAccount, settings, positionSize,
                currentPremium, orderResponse.getOrderid(), isReentry);

        // Step 8: persist position (live tracking state)
        persistPosition(trade, positionSize.totalQuantity(), trade.getStopLossPrice());

        log.info("[{}] Trade executed: orderId={} symbol={} qty={} at ₹{}",
                signal.getIndexName(), orderResponse.getOrderid(),
                signal.getTradingSymbol(), positionSize.totalQuantity(), currentPremium);

        return TradeExecutionResult.success(trade);
    }

    private PlaceOrderRequest buildOrderRequest(Signal signal, PositionSize positionSize) {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setTradingsymbol(signal.getTradingSymbol());
        req.setSymboltoken(signal.getSymbolToken());
        req.setTransactiontype(TransactionType.BUY.name());
        req.setExchange("NFO");
        req.setOrdertype("MARKET");
        req.setProducttype("INTRADAY");
        req.setDuration("DAY");
        req.setQuantity(String.valueOf(positionSize.totalQuantity()));
        req.setPrice("0");
        return req;
    }

    private Trade persistTrade(Signal signal,
                                BrokerAccount brokerAccount,
                                StrategySettings settings,
                                PositionSize positionSize,
                                BigDecimal entryPrice,
                                String brokerOrderId,
                                boolean isReentry) {
        BigDecimal sl = entryPrice.subtract(settings.getStopLossPoints());
        BigDecimal t1 = entryPrice.add(settings.getTarget1Points());
        BigDecimal t2 = entryPrice.add(settings.getTarget2Points());

        Trade trade = new Trade();
        trade.setSignal(signal);
        trade.setBrokerAccount(brokerAccount);
        trade.setIndexName(signal.getIndexName());
        trade.setTradingSymbol(signal.getTradingSymbol());
        trade.setSymbolToken(signal.getSymbolToken());
        trade.setTransactionType(TransactionType.BUY);
        trade.setQuantity(positionSize.totalQuantity());
        trade.setEntryPrice(entryPrice);
        trade.setStopLossPrice(sl);
        trade.setTarget1Price(t1);
        trade.setTarget2Price(t2);
        trade.setBrokerOrderId(brokerOrderId);
        trade.setStatus(TradeStatus.OPEN);
        trade.setReentry(isReentry);
        trade.setEntryTime(Instant.now());

        return tradeRepository.save(trade);
    }

    private void persistPosition(Trade trade, int quantity, BigDecimal initialStopLoss) {
        Position position = new Position();
        position.setTrade(trade);
        position.setQuantityRemaining(quantity);
        position.setCurrentStopLoss(initialStopLoss);
        position.setSlMovedToCost(false);
        positionRepository.save(position);
    }
}
