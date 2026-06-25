package com.tradingplatform.exit;

import com.tradingplatform.angelone.AngelOneOrderClient;
import com.tradingplatform.angelone.dto.PlaceOrderRequest;
import com.tradingplatform.domain.*;
import com.tradingplatform.domain.enums.*;
import com.tradingplatform.repository.PositionRepository;
import com.tradingplatform.repository.StrategySettingsRepository;
import com.tradingplatform.repository.TradeRepository;
import com.tradingplatform.risk.RiskManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * BRD Section 7 — Exit Strategy.
 *
 * Option 1 (default):
 *   - LTP hits Target 1  → close 50% quantity, move SL to cost price (entry price)
 *   - LTP then hits Target 2 → close remaining 50%, mark CLOSED
 *   - LTP hits SL at any point → close all remaining, mark CLOSED
 *
 * Option 2:
 *   - LTP hits Target 1  → close 100% quantity immediately, mark CLOSED
 *   - No partial close, no trailing SL
 *
 * The monitor() method is called on every tick (or every 15-min candle close)
 * for each open position. It evaluates the current LTP against the position's
 * levels and takes action if a level is hit.
 *
 * This service is also responsible for updating Position.currentLtp,
 * Position.currentStopLoss (after SL move to cost), and Position.slMovedToCost.
 */
@Service
public class ExitStrategyService {

    private static final Logger log = LoggerFactory.getLogger(ExitStrategyService.class);

    private final StrategySettingsRepository strategySettingsRepository;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final AngelOneOrderClient orderClient;
    private final RiskManagementService riskManagementService;

    public ExitStrategyService(StrategySettingsRepository strategySettingsRepository,
                                TradeRepository tradeRepository,
                                PositionRepository positionRepository,
                                AngelOneOrderClient orderClient,
                                RiskManagementService riskManagementService) {
        this.strategySettingsRepository = strategySettingsRepository;
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
        this.orderClient = orderClient;
        this.riskManagementService = riskManagementService;
    }

    /**
     * Evaluates exit conditions for a single open position given the current LTP.
     * Called on each price update (tick or candle close).
     *
     * @param trade      the open trade to evaluate
     * @param position   the live position state (quantity remaining, current SL)
     * @param currentLtp the option's last traded price right now
     */
    @Transactional
    public ExitResult monitor(Trade trade, Position position, BigDecimal currentLtp) {
        if (trade.getStatus() != TradeStatus.OPEN
                && trade.getStatus() != TradeStatus.PARTIALLY_CLOSED) {
            return ExitResult.hold(currentLtp);
        }

        // Update LTP on the position regardless of whether we exit
        position.setCurrentLtp(currentLtp);

        StrategySettings settings = strategySettingsRepository
                .findByBrokerAccountIdAndIndexName(
                        trade.getBrokerAccount().getId(), trade.getIndexName())
                .orElseThrow(() -> new IllegalStateException(
                        "No strategy settings for trade " + trade.getId()));

        BigDecimal effectiveSL = position.getCurrentStopLoss() != null
                ? position.getCurrentStopLoss()
                : trade.getStopLossPrice();

        // Priority: SL first, then Target 2, then Target 1
        ExitTrigger trigger = evaluateTrigger(currentLtp, effectiveSL,
                trade.getTarget1Price(), trade.getTarget2Price(),
                trade.getStatus() == TradeStatus.PARTIALLY_CLOSED);

        return switch (trigger) {
            case STOP_LOSS -> handleStopLoss(trade, position, currentLtp, settings);
            case TARGET2   -> handleTarget2(trade, position, currentLtp, settings);
            case TARGET1   -> handleTarget1(trade, position, currentLtp, settings);
            case NONE      -> {
                positionRepository.save(position);
                yield ExitResult.hold(currentLtp);
            }
        };
    }

    /**
     * Evaluates which level (if any) was hit.
     * After a partial close (SL moved to cost), we only check SL and Target 2.
     */
    private ExitTrigger evaluateTrigger(BigDecimal ltp,
                                         BigDecimal stopLoss,
                                         BigDecimal target1,
                                         BigDecimal target2,
                                         boolean partiallyFilled) {
        // SL check always takes priority
        if (ltp.compareTo(stopLoss) <= 0) {
            return ExitTrigger.STOP_LOSS;
        }
        // Target 2 only relevant after Target 1 hit
        if (partiallyFilled && target2 != null && ltp.compareTo(target2) >= 0) {
            return ExitTrigger.TARGET2;
        }
        // Target 1 only relevant if not already partially filled
        if (!partiallyFilled && target1 != null && ltp.compareTo(target1) >= 0) {
            return ExitTrigger.TARGET1;
        }
        return ExitTrigger.NONE;
    }

    // -------------------------------------------------------------------------
    // Target 1 hit
    // -------------------------------------------------------------------------

    private ExitResult handleTarget1(Trade trade, Position position,
                                      BigDecimal exitPrice, StrategySettings settings) {
        if (settings.getExitStrategyMode() == ExitStrategyMode.OPTION2) {
            // Option 2: close 100% at Target 1
            log.info("[{}] Target 1 hit (Option 2) — closing full position at ₹{}",
                    trade.getIndexName(), exitPrice);
            return closePosition(trade, position, exitPrice,
                    ExitTrigger.TARGET1, ExitReason.TARGET1, position.getQuantityRemaining());
        }

        // Option 1: close 50%, move SL to cost
        int halfQty = position.getQuantityRemaining() / 2;
        if (halfQty == 0) {
            // Odd lot — close all
            return closePosition(trade, position, exitPrice,
                    ExitTrigger.TARGET1, ExitReason.TARGET1, position.getQuantityRemaining());
        }

        log.info("[{}] Target 1 hit (Option 1) — closing {} of {} units at ₹{}, moving SL to cost ₹{}",
                trade.getIndexName(), halfQty, position.getQuantityRemaining(),
                exitPrice, trade.getEntryPrice());

        // Place SELL order for half
        placeExitOrder(trade, halfQty, exitPrice);

        BigDecimal partialPnl = exitPrice.subtract(trade.getEntryPrice())
                .multiply(BigDecimal.valueOf(halfQty));

        // Update position: remaining qty, SL moved to entry price (cost)
        position.setQuantityRemaining(position.getQuantityRemaining() - halfQty);
        position.setCurrentStopLoss(trade.getEntryPrice()); // SL → cost
        position.setSlMovedToCost(true);
        positionRepository.save(position);

        // Mark trade as partially closed
        trade.setStatus(TradeStatus.PARTIALLY_CLOSED);
        tradeRepository.save(trade);

        return ExitResult.partialClose(exitPrice, halfQty, partialPnl, trade);
    }

    // -------------------------------------------------------------------------
    // Target 2 hit
    // -------------------------------------------------------------------------

    private ExitResult handleTarget2(Trade trade, Position position,
                                      BigDecimal exitPrice, StrategySettings settings) {
        log.info("[{}] Target 2 hit — closing remaining {} units at ₹{}",
                trade.getIndexName(), position.getQuantityRemaining(), exitPrice);
        return closePosition(trade, position, exitPrice,
                ExitTrigger.TARGET2, ExitReason.TARGET2, position.getQuantityRemaining());
    }

    // -------------------------------------------------------------------------
    // Stop loss hit
    // -------------------------------------------------------------------------

    private ExitResult handleStopLoss(Trade trade, Position position,
                                       BigDecimal exitPrice, StrategySettings settings) {
        log.info("[{}] Stop loss hit — closing {} units at ₹{} (SL=₹{})",
                trade.getIndexName(), position.getQuantityRemaining(),
                exitPrice, position.getCurrentStopLoss());
        return closePosition(trade, position, exitPrice,
                ExitTrigger.STOP_LOSS, ExitReason.STOP_LOSS, position.getQuantityRemaining());
    }

    // -------------------------------------------------------------------------
    // Close all (or specified qty)
    // -------------------------------------------------------------------------

    private ExitResult closePosition(Trade trade, Position position,
                                      BigDecimal exitPrice, ExitTrigger trigger,
                                      ExitReason reason, int qty) {
        placeExitOrder(trade, qty, exitPrice);

        BigDecimal pnl = exitPrice.subtract(trade.getEntryPrice())
                .multiply(BigDecimal.valueOf(qty));

        // Update trade
        trade.setExitPrice(exitPrice);
        trade.setExitReason(reason);
        trade.setRealizedPnl(pnl);
        trade.setStatus(TradeStatus.CLOSED);
        trade.setExitTime(Instant.now());
        tradeRepository.save(trade);

        // Update position
        position.setQuantityRemaining(0);
        position.setUnrealizedPnl(BigDecimal.ZERO);
        positionRepository.save(position);

        // Record P&L in risk module so daily loss cap stays accurate
        riskManagementService.recordTradeResult(trade.getBrokerAccount().getId(), pnl);

        return ExitResult.fullClose(trigger, reason, exitPrice, qty, pnl, trade);
    }

    private void placeExitOrder(Trade trade, int qty, BigDecimal exitPrice) {
        try {
            PlaceOrderRequest req = new PlaceOrderRequest();
            req.setTradingsymbol(trade.getTradingSymbol());
            req.setSymboltoken(trade.getSymbolToken());
            req.setTransactiontype(TransactionType.SELL.name());
            req.setExchange("NFO");
            req.setOrdertype("MARKET");
            req.setProducttype("INTRADAY");
            req.setDuration("DAY");
            req.setQuantity(String.valueOf(qty));
            req.setPrice("0");
            orderClient.placeOrder(req);
        } catch (Exception e) {
            // Log but don't block — position must be marked closed
            // even if the broker order fails (manual reconciliation needed)
            log.error("[{}] Exit order placement failed for trade {}: {}",
                    trade.getIndexName(), trade.getId(), e.getMessage());
        }
    }

    /**
     * Convenience method: evaluate all open positions for an account.
     * Called by the scheduler on each tick/candle.
     */
    @Transactional
    public List<ExitResult> monitorAll(Long brokerAccountId,
                                        java.util.function.Function<Trade, BigDecimal> ltpProvider) {
        List<Trade> openTrades = tradeRepository.findByBrokerAccountIdAndStatus(
                brokerAccountId, TradeStatus.OPEN);
        openTrades.addAll(tradeRepository.findByBrokerAccountIdAndStatus(
                brokerAccountId, TradeStatus.PARTIALLY_CLOSED));

        return openTrades.stream().map(trade -> {
            Position position = positionRepository.findByTradeId(trade.getId())
                    .orElse(null);
            if (position == null) return ExitResult.hold(BigDecimal.ZERO);

            BigDecimal ltp = ltpProvider.apply(trade);
            if (ltp == null) return ExitResult.hold(BigDecimal.ZERO);

            return monitor(trade, position, ltp);
        }).toList();
    }
}
