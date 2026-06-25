package com.tradingplatform.exit;

import com.tradingplatform.domain.Trade;
import com.tradingplatform.domain.enums.ExitReason;

import java.math.BigDecimal;

/**
 * Result of evaluating exit conditions for an open position.
 *
 * @param actionTaken   what the exit service did
 * @param trigger       which level was hit
 * @param exitReason    maps to Trade.exitReason for DB persistence
 * @param exitPrice     the LTP at which the exit was evaluated
 * @param quantityClosed  how many units were closed (0 if HOLD)
 * @param realizedPnl   P&L for the closed portion (null if HOLD)
 * @param trade         the updated trade entity (null if HOLD)
 */
public record ExitResult(
        ExitAction actionTaken,
        ExitTrigger trigger,
        ExitReason exitReason,
        BigDecimal exitPrice,
        int quantityClosed,
        BigDecimal realizedPnl,
        Trade trade
) {
    public enum ExitAction {
        HOLD,               // No level hit — do nothing
        PARTIAL_CLOSE,      // Target 1 hit (Option 1): close 50%, move SL to cost
        FULL_CLOSE,         // Stop loss hit, Target 1 hit (Option 2), or Target 2 hit
    }

    public static ExitResult hold(BigDecimal currentLtp) {
        return new ExitResult(ExitAction.HOLD, ExitTrigger.NONE, null, currentLtp, 0, null, null);
    }

    public static ExitResult partialClose(BigDecimal exitPrice, int qty, BigDecimal pnl, Trade trade) {
        return new ExitResult(ExitAction.PARTIAL_CLOSE, ExitTrigger.TARGET1,
                ExitReason.TARGET1, exitPrice, qty, pnl, trade);
    }

    public static ExitResult fullClose(ExitTrigger trigger, ExitReason reason,
                                        BigDecimal exitPrice, int qty, BigDecimal pnl, Trade trade) {
        return new ExitResult(ExitAction.FULL_CLOSE, trigger, reason, exitPrice, qty, pnl, trade);
    }
}
