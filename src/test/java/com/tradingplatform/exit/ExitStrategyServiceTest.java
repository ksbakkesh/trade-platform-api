package com.tradingplatform.exit;

import com.tradingplatform.angelone.AngelOneOrderClient;
import com.tradingplatform.domain.*;
import com.tradingplatform.domain.enums.*;
import com.tradingplatform.repository.PositionRepository;
import com.tradingplatform.repository.StrategySettingsRepository;
import com.tradingplatform.repository.TradeRepository;
import com.tradingplatform.risk.RiskManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExitStrategyServiceTest {

    @Mock private StrategySettingsRepository strategySettingsRepository;
    @Mock private TradeRepository tradeRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private AngelOneOrderClient orderClient;
    @Mock private RiskManagementService riskManagementService;

    @InjectMocks
    private ExitStrategyService service;

    private Trade trade;
    private Position position;
    private BrokerAccount account;
    private StrategySettings settingsOption1;
    private StrategySettings settingsOption2;

    @BeforeEach
    void setup() {
        account = new BrokerAccount();
        account.setId(1L);

        // Trade: bought at ₹150, SL=₹50, T1=₹310, T2=₹350
        trade = new Trade();
        trade.setId(1L);
        trade.setBrokerAccount(account);
        trade.setIndexName(IndexName.NIFTY);
        trade.setTradingSymbol("NIFTY28NOV2424500CE");
        trade.setSymbolToken("12345");
        trade.setTransactionType(TransactionType.BUY);
        trade.setQuantity(150);
        trade.setEntryPrice(new BigDecimal("150"));
        trade.setStopLossPrice(new BigDecimal("50"));
        trade.setTarget1Price(new BigDecimal("310"));
        trade.setTarget2Price(new BigDecimal("350"));
        trade.setStatus(TradeStatus.OPEN);

        position = new Position();
        position.setId(1L);
        position.setTrade(trade);
        position.setQuantityRemaining(150);
        position.setCurrentStopLoss(new BigDecimal("50"));
        position.setSlMovedToCost(false);

        settingsOption1 = new StrategySettings();
        settingsOption1.setExitStrategyMode(ExitStrategyMode.OPTION1);

        settingsOption2 = new StrategySettings();
        settingsOption2.setExitStrategyMode(ExitStrategyMode.OPTION2);

        when(strategySettingsRepository.findByBrokerAccountIdAndIndexName(any(), any()))
                .thenReturn(Optional.of(settingsOption1));
        when(tradeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // HOLD — price between SL and T1
    // -------------------------------------------------------------------------

    @Test
    void holdsWhenPriceBetweenSlAndTarget1() {
        ExitResult result = service.monitor(trade, position, new BigDecimal("200"));

        assertEquals(ExitResult.ExitAction.HOLD, result.actionTaken());
        assertEquals(ExitTrigger.NONE, result.trigger());
        verify(orderClient, never()).placeOrder(any());
    }

    @Test
    void holdsExactlyAtEntryPrice() {
        ExitResult result = service.monitor(trade, position, new BigDecimal("150"));
        assertEquals(ExitResult.ExitAction.HOLD, result.actionTaken());
    }

    // -------------------------------------------------------------------------
    // STOP LOSS
    // -------------------------------------------------------------------------

    @Test
    void closesFullPositionOnStopLoss() {
        ExitResult result = service.monitor(trade, position, new BigDecimal("50")); // exactly at SL

        assertEquals(ExitResult.ExitAction.FULL_CLOSE, result.actionTaken());
        assertEquals(ExitTrigger.STOP_LOSS, result.trigger());
        assertEquals(ExitReason.STOP_LOSS, result.exitReason());
        assertEquals(150, result.quantityClosed());
        // P&L = (50 - 150) × 150 = -₹15,000
        assertEquals(new BigDecimal("-15000"), result.realizedPnl());
        assertEquals(TradeStatus.CLOSED, result.trade().getStatus());
        verify(orderClient).placeOrder(any());
        verify(riskManagementService).recordTradeResult(eq(1L), eq(new BigDecimal("-15000")));
    }

    @Test
    void closesWhenPriceBelowStopLoss() {
        ExitResult result = service.monitor(trade, position, new BigDecimal("30")); // below SL
        assertEquals(ExitResult.ExitAction.FULL_CLOSE, result.actionTaken());
        assertEquals(ExitTrigger.STOP_LOSS, result.trigger());
    }

    // -------------------------------------------------------------------------
    // TARGET 1 — Option 1 (partial close + SL move to cost)
    // -------------------------------------------------------------------------

    @Test
    void option1_closesHalfAndMovesSLToCostOnTarget1() {
        ExitResult result = service.monitor(trade, position, new BigDecimal("310")); // T1

        assertEquals(ExitResult.ExitAction.PARTIAL_CLOSE, result.actionTaken());
        assertEquals(ExitTrigger.TARGET1, result.trigger());
        assertEquals(75, result.quantityClosed()); // 50% of 150
        // P&L for 75 units: (310 - 150) × 75 = ₹12,000
        assertEquals(new BigDecimal("12000"), result.realizedPnl());

        // SL must now be at entry price (cost)
        assertEquals(new BigDecimal("150"), position.getCurrentStopLoss());
        assertTrue(position.isSlMovedToCost());
        assertEquals(75, position.getQuantityRemaining());
        assertEquals(TradeStatus.PARTIALLY_CLOSED, result.trade().getStatus());

        verify(orderClient).placeOrder(any());
        // No risk record yet — trade not closed
        verify(riskManagementService, never()).recordTradeResult(any(), any());
    }

    @Test
    void option1_closesFullOnTarget2AfterPartialClose() {
        // Simulate state after Target 1 was already hit
        trade.setStatus(TradeStatus.PARTIALLY_CLOSED);
        position.setQuantityRemaining(75);
        position.setCurrentStopLoss(new BigDecimal("150")); // SL moved to cost
        position.setSlMovedToCost(true);

        ExitResult result = service.monitor(trade, position, new BigDecimal("350")); // T2

        assertEquals(ExitResult.ExitAction.FULL_CLOSE, result.actionTaken());
        assertEquals(ExitTrigger.TARGET2, result.trigger());
        assertEquals(ExitReason.TARGET2, result.exitReason());
        assertEquals(75, result.quantityClosed());
        // P&L: (350 - 150) × 75 = ₹15,000
        assertEquals(new BigDecimal("15000"), result.realizedPnl());
        assertEquals(TradeStatus.CLOSED, result.trade().getStatus());
        verify(riskManagementService).recordTradeResult(eq(1L), eq(new BigDecimal("15000")));
    }

    @Test
    void option1_slHitAfterPartialClose_closesRemaining() {
        // SL was moved to cost (₹150), price drops back to entry
        trade.setStatus(TradeStatus.PARTIALLY_CLOSED);
        position.setQuantityRemaining(75);
        position.setCurrentStopLoss(new BigDecimal("150")); // cost-based SL
        position.setSlMovedToCost(true);

        ExitResult result = service.monitor(trade, position, new BigDecimal("150")); // exactly at moved SL

        assertEquals(ExitResult.ExitAction.FULL_CLOSE, result.actionTaken());
        assertEquals(ExitTrigger.STOP_LOSS, result.trigger());
        assertEquals(75, result.quantityClosed());
        // P&L: (150 - 150) × 75 = ₹0 (break even — SL at cost)
        assertEquals(new BigDecimal("0"), result.realizedPnl());
    }

    // -------------------------------------------------------------------------
    // TARGET 1 — Option 2 (full close immediately)
    // -------------------------------------------------------------------------

    @Test
    void option2_closesFullOnTarget1() {
        when(strategySettingsRepository.findByBrokerAccountIdAndIndexName(any(), any()))
                .thenReturn(Optional.of(settingsOption2));

        ExitResult result = service.monitor(trade, position, new BigDecimal("310")); // T1

        assertEquals(ExitResult.ExitAction.FULL_CLOSE, result.actionTaken());
        assertEquals(ExitTrigger.TARGET1, result.trigger());
        assertEquals(ExitReason.TARGET1, result.exitReason());
        assertEquals(150, result.quantityClosed()); // 100%, not 50%
        assertEquals(TradeStatus.CLOSED, result.trade().getStatus());
        verify(riskManagementService).recordTradeResult(any(), any());
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    void doesNotEvaluateClosedTrade() {
        trade.setStatus(TradeStatus.CLOSED);

        ExitResult result = service.monitor(trade, position, new BigDecimal("50")); // below SL

        assertEquals(ExitResult.ExitAction.HOLD, result.actionTaken());
        verify(orderClient, never()).placeOrder(any());
    }

    @Test
    void target1NotTriggeredOnPartiallyClosedTrade() {
        // After T1 hit, we should NOT re-trigger T1 again
        trade.setStatus(TradeStatus.PARTIALLY_CLOSED);
        position.setQuantityRemaining(75);
        position.setCurrentStopLoss(new BigDecimal("150"));
        position.setSlMovedToCost(true);

        // Price at T1 again — should HOLD, not partial close again
        ExitResult result = service.monitor(trade, position, new BigDecimal("310"));

        assertEquals(ExitResult.ExitAction.HOLD, result.actionTaken());
    }

    @Test
    void exitOrderFailureDoesNotPreventTradeClose() {
        doThrow(new RuntimeException("Angel One down")).when(orderClient).placeOrder(any());

        // Should still close the trade in DB even if broker call fails
        ExitResult result = service.monitor(trade, position, new BigDecimal("50"));

        assertEquals(ExitResult.ExitAction.FULL_CLOSE, result.actionTaken());
        assertEquals(TradeStatus.CLOSED, result.trade().getStatus());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Long eq(Long val) {
        return org.mockito.ArgumentMatchers.eq(val);
    }

    private static BigDecimal eq(BigDecimal val) {
        return org.mockito.ArgumentMatchers.eq(val);
    }
}
