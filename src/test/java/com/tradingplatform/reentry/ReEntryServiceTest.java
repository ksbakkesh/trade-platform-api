package com.tradingplatform.reentry;

import com.tradingplatform.domain.*;
import com.tradingplatform.domain.enums.*;
import com.tradingplatform.exit.ExitResult;
import com.tradingplatform.exit.ExitTrigger;
import com.tradingplatform.repository.StrategySettingsRepository;
import com.tradingplatform.repository.TradeRepository;
import com.tradingplatform.risk.RiskCheckResult;
import com.tradingplatform.risk.RiskManagementService;
import com.tradingplatform.signal.*;
import com.tradingplatform.trade.TradeExecutionResult;
import com.tradingplatform.trade.TradeExecutionService;
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
class ReEntryServiceTest {

    @Mock private StrategySettingsRepository strategySettingsRepository;
    @Mock private RiskManagementService riskManagementService;
    @Mock private EntryConditionChecker entryConditionChecker;
    @Mock private SignalGenerationService signalGenerationService;
    @Mock private TradeExecutionService tradeExecutionService;
    @Mock private TradeRepository tradeRepository;

    @InjectMocks
    private ReEntryService service;

    private BrokerAccount account;
    private Trade stoppedTrade;
    private Signal originalSignal;
    private StrategySettings settings;
    private MarketSnapshot goodSnapshot;
    private ExitResult slExitResult;

    @BeforeEach
    void setup() {
        account = new BrokerAccount();
        account.setId(1L);

        originalSignal = new Signal();
        originalSignal.setSignalType(OptionType.CE);
        originalSignal.setIndexName(IndexName.NIFTY);

        stoppedTrade = new Trade();
        stoppedTrade.setId(1L);
        stoppedTrade.setBrokerAccount(account);
        stoppedTrade.setIndexName(IndexName.NIFTY);
        stoppedTrade.setTradingSymbol("NIFTY28NOV2424500CE");
        stoppedTrade.setSymbolToken("12345");
        stoppedTrade.setSignal(originalSignal);
        stoppedTrade.setStatus(TradeStatus.CLOSED);
        stoppedTrade.setExitReason(ExitReason.STOP_LOSS);

        settings = new StrategySettings();
        settings.setReEntryEnabled(true);
        settings.setAutoTradingEnabled(true);
        settings.setPremiumThreshold(new BigDecimal("125"));
        settings.setRsiThreshold(new BigDecimal("60"));
        settings.setVolumeMultiplier(new BigDecimal("2"));
        settings.setDeltaMin(new BigDecimal("0.45"));
        settings.setDeltaMax(new BigDecimal("0.65"));
        settings.setQuantityMode(QuantityMode.CAPITAL_BASED);
        settings.setCapitalAllocationPercent(new BigDecimal("20"));
        settings.setStopLossPoints(new BigDecimal("100"));
        settings.setTarget1Points(new BigDecimal("160"));
        settings.setTarget2Points(new BigDecimal("200"));

        goodSnapshot = new MarketSnapshot(
                new BigDecimal("150"), 20000L, 8000L,
                new BigDecimal("65"), new BigDecimal("0.55"));

        // A stop-loss exit result
        slExitResult = ExitResult.fullClose(ExitTrigger.STOP_LOSS, ExitReason.STOP_LOSS,
                new BigDecimal("50"), 150, new BigDecimal("-15000"), stoppedTrade);

        when(strategySettingsRepository.findByBrokerAccountIdAndIndexName(any(), any()))
                .thenReturn(Optional.of(settings));
        when(riskManagementService.checkCanTrade(1L))
                .thenReturn(new RiskCheckResult(true, "Trading allowed", 1, 2,
                        new BigDecimal("1500"), new BigDecimal("4500")));
        when(entryConditionChecker.check(any(), any(), any()))
                .thenReturn(EntryConditionResult.passed(OptionType.CE,
                        new BigDecimal("150"), new BigDecimal("65"),
                        new BigDecimal("2.5"), new BigDecimal("0.55")));

        Signal reEntrySignal = new Signal();
        reEntrySignal.setId(10L);
        reEntrySignal.setStatus(SignalStatus.GENERATED);
        reEntrySignal.setSignalType(OptionType.CE);
        reEntrySignal.setIndexName(IndexName.NIFTY);
        reEntrySignal.setTradingSymbol("NIFTY28NOV2424500CE");
        reEntrySignal.setSymbolToken("12345");
        reEntrySignal.setStrikePrice(new BigDecimal("24500"));
        when(signalGenerationService.generate(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(reEntrySignal);

        Trade reEntryTrade = new Trade();
        reEntryTrade.setId(2L);
        reEntryTrade.setBrokerOrderId("RE123456");
        when(tradeExecutionService.execute(any(), any(), any(), any(), eq(true), eq(false)))
                .thenReturn(TradeExecutionResult.success(reEntryTrade));
    }

    @Test
    void allowsReEntryWhenAllConditionsMet() {
        ReEntryResult result = service.evaluate(slExitResult, stoppedTrade, account,
                new BigDecimal("24350"), new BigDecimal("24360"),
                goodSnapshot, new BigDecimal("100000"));

        assertTrue(result.allowed());
        assertTrue(result.reason().contains("Re-entry placed successfully"));
        verify(signalGenerationService).generate(any(), any(), any(), any(), any(), any(), any());
        verify(tradeExecutionService).execute(any(), any(), any(), any(), eq(true), eq(false));
    }

    @Test
    void blocksReEntryWhenNotStopLossExit() {
        ExitResult target1Exit = ExitResult.fullClose(ExitTrigger.TARGET1, ExitReason.TARGET1,
                new BigDecimal("310"), 150, new BigDecimal("24000"), stoppedTrade);

        ReEntryResult result = service.evaluate(target1Exit, stoppedTrade, account,
                new BigDecimal("24350"), new BigDecimal("24360"),
                goodSnapshot, new BigDecimal("100000"));

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("stop-loss exits"));
        verify(signalGenerationService, never()).generate(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void blocksReEntryWhenDisabledInSettings() {
        settings.setReEntryEnabled(false);

        ReEntryResult result = service.evaluate(slExitResult, stoppedTrade, account,
                new BigDecimal("24350"), new BigDecimal("24360"),
                goodSnapshot, new BigDecimal("100000"));

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("disabled"));
    }

    @Test
    void blocksReEntryWhenRiskCapHit() {
        when(riskManagementService.checkCanTrade(1L))
                .thenReturn(new RiskCheckResult(false, "Max trades per day reached (2/2)",
                        2, 2, BigDecimal.ZERO, new BigDecimal("4500")));

        ReEntryResult result = service.evaluate(slExitResult, stoppedTrade, account,
                new BigDecimal("24350"), new BigDecimal("24360"),
                goodSnapshot, new BigDecimal("100000"));

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Risk check failed"));
    }

    @Test
    void blocksReEntryWhenEntryConditionsNotMet() {
        when(entryConditionChecker.check(any(), any(), any()))
                .thenReturn(EntryConditionResult.rejected(OptionType.CE,
                        "RSI 45 ≤ threshold 60",
                        new BigDecimal("150"), new BigDecimal("45"),
                        new BigDecimal("2.5"), new BigDecimal("0.55")));

        ReEntryResult result = service.evaluate(slExitResult, stoppedTrade, account,
                new BigDecimal("24350"), new BigDecimal("24360"),
                goodSnapshot, new BigDecimal("100000"));

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Entry conditions not met"));
        verify(signalGenerationService, never()).generate(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void blocksReEntryWhenSignalGenerationFails() {
        Signal rejectedSignal = new Signal();
        rejectedSignal.setStatus(SignalStatus.REJECTED);
        rejectedSignal.setRejectionReason("Spot between Buy Above and Sell Below");
        when(signalGenerationService.generate(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(rejectedSignal);

        ReEntryResult result = service.evaluate(slExitResult, stoppedTrade, account,
                new BigDecimal("24350"), new BigDecimal("24360"),
                goodSnapshot, new BigDecimal("100000"));

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Signal generation rejected"));
    }

    @Test
    void blocksReEntryWhenExecutionFails() {
        when(tradeExecutionService.execute(any(), any(), any(), any(), eq(true), eq(false)))
                .thenReturn(TradeExecutionResult.blocked("Risk check failed"));

        ReEntryResult result = service.evaluate(slExitResult, stoppedTrade, account,
                new BigDecimal("24350"), new BigDecimal("24360"),
                goodSnapshot, new BigDecimal("100000"));

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Re-entry order failed"));
    }

    @Test
    void reEntryTradeIsMarkedAsReentry() {
        service.evaluate(slExitResult, stoppedTrade, account,
                new BigDecimal("24350"), new BigDecimal("24360"),
                goodSnapshot, new BigDecimal("100000"));

        // Verify tradeExecutionService was called with isReentry=true
        verify(tradeExecutionService).execute(any(), any(), any(), any(), eq(true), eq(false));
    }

    // Mockito helpers
    private static boolean eq(boolean val) {
        return org.mockito.ArgumentMatchers.eq(val);
    }
}
