package com.tradingplatform.risk;

import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.DailyPnl;
import com.tradingplatform.domain.RiskSettings;
import com.tradingplatform.repository.BrokerAccountRepository;
import com.tradingplatform.repository.DailyPnlRepository;
import com.tradingplatform.repository.RiskSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RiskManagementServiceTest {

    @Mock private RiskSettingsRepository riskSettingsRepository;
    @Mock private DailyPnlRepository dailyPnlRepository;
    @Mock private BrokerAccountRepository brokerAccountRepository;

    @InjectMocks
    private RiskManagementService service;

    private static final Long ACCOUNT_ID = 1L;
    private RiskSettings defaultSettings;

    @BeforeEach
    void setup() {
        defaultSettings = new RiskSettings();
        BrokerAccount account = new BrokerAccount();
        account.setId(ACCOUNT_ID);
        defaultSettings.setBrokerAccount(account);
        defaultSettings.setMaxTradesPerDay(2);
        defaultSettings.setDailyLossLimit(new BigDecimal("4500"));
        defaultSettings.setScope("COMBINED");

        when(riskSettingsRepository.findByBrokerAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(defaultSettings));

        BrokerAccount ref = new BrokerAccount();
        ref.setId(ACCOUNT_ID);
        when(brokerAccountRepository.getReferenceById(ACCOUNT_ID)).thenReturn(ref);
    }

    @Test
    void allowsTradeWhenNothingTradedToday() {
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.of(freshDailyPnl(0, BigDecimal.ZERO)));

        RiskCheckResult result = service.checkCanTrade(ACCOUNT_ID);

        assertTrue(result.allowed());
        assertEquals(0, result.tradesUsedToday());
        assertEquals(2, result.maxTradesPerDay());
        assertEquals(BigDecimal.ZERO, result.lossUsedToday());
        assertEquals(2, result.remainingTrades());
    }

    @Test
    void allowsTradeAfterOneProfitableTrade() {
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.of(freshDailyPnl(1, new BigDecimal("800"))));

        RiskCheckResult result = service.checkCanTrade(ACCOUNT_ID);

        assertTrue(result.allowed());
        assertEquals(1, result.tradesUsedToday());
        assertEquals(1, result.remainingTrades());
        assertEquals(BigDecimal.ZERO, result.lossUsedToday());
    }

    @Test
    void allowsTradeWhenLossBelowLimit() {
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.of(freshDailyPnl(1, new BigDecimal("-4499"))));

        RiskCheckResult result = service.checkCanTrade(ACCOUNT_ID);

        assertTrue(result.allowed());
        assertEquals(new BigDecimal("4499"), result.lossUsedToday());
        assertEquals(new BigDecimal("1"), result.remainingLossBudget());
    }

    @Test
    void blocksTradeWhenMaxTradesReached() {
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.of(freshDailyPnl(2, BigDecimal.ZERO)));

        RiskCheckResult result = service.checkCanTrade(ACCOUNT_ID);

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Max trades per day reached"));
        assertEquals(0, result.remainingTrades());
    }

    @Test
    void blocksTradeWhenDailyLossLimitExactlyHit() {
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.of(freshDailyPnl(1, new BigDecimal("-4500"))));

        RiskCheckResult result = service.checkCanTrade(ACCOUNT_ID);

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Daily loss limit hit"));
        assertEquals(BigDecimal.ZERO, result.remainingLossBudget());
    }

    @Test
    void blocksTradeWhenDailyLossLimitExceeded() {
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.of(freshDailyPnl(1, new BigDecimal("-5000"))));

        RiskCheckResult result = service.checkCanTrade(ACCOUNT_ID);

        assertFalse(result.allowed());
        assertEquals(BigDecimal.ZERO, result.remainingLossBudget());
    }

    @Test
    void blocksTradeWhenManuallyDisabled() {
        DailyPnl pnl = freshDailyPnl(0, BigDecimal.ZERO);
        pnl.setTradingDisabled(true);
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.of(pnl));

        RiskCheckResult result = service.checkCanTrade(ACCOUNT_ID);

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("disabled"));
    }

    @Test
    void recordsLossAndUpdatesTradeCount() {
        DailyPnl pnl = freshDailyPnl(1, new BigDecimal("-1000"));
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.of(pnl));
        when(dailyPnlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordTradeResult(ACCOUNT_ID, new BigDecimal("-2000"));

        assertEquals(2, pnl.getTotalTrades());
        assertEquals(new BigDecimal("-3000"), pnl.getTotalPnl());
        assertTrue(pnl.isMaxTradesHit());
        assertFalse(pnl.isDailyLossLimitHit());
    }

    @Test
    void setsLossLimitFlagWhenLossExceedsLimit() {
        DailyPnl pnl = freshDailyPnl(1, new BigDecimal("-1000"));
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.of(pnl));
        when(dailyPnlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordTradeResult(ACCOUNT_ID, new BigDecimal("-3600"));

        assertTrue(pnl.isDailyLossLimitHit());
    }

    @Test
    void profitDoesNotConsumeLossBudget() {
        DailyPnl pnl = freshDailyPnl(0, BigDecimal.ZERO);
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.of(pnl));
        when(dailyPnlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordTradeResult(ACCOUNT_ID, new BigDecimal("1500"));

        assertEquals(new BigDecimal("1500"), pnl.getTotalPnl());
        assertFalse(pnl.isDailyLossLimitHit());
    }

    @Test
    void createsNewDailyPnlRowForFreshDay() {
        when(dailyPnlRepository.findByBrokerAccountIdAndTradeDate(eq(ACCOUNT_ID), any()))
                .thenReturn(Optional.empty());
        when(dailyPnlRepository.save(any(DailyPnl.class))).thenAnswer(inv -> inv.getArgument(0));

        RiskCheckResult result = service.checkCanTrade(ACCOUNT_ID);

        assertTrue(result.allowed());
        verify(dailyPnlRepository, times(1)).save(any(DailyPnl.class));
    }

    @Test
    void remainingLossBudgetIsZeroWhenOverLimit() {
        RiskCheckResult result = new RiskCheckResult(
                false, "blocked", 1, 2, new BigDecimal("5000"), new BigDecimal("4500"));
        assertEquals(BigDecimal.ZERO, result.remainingLossBudget());
    }

    private static DailyPnl freshDailyPnl(int trades, BigDecimal totalPnl) {
        DailyPnl pnl = new DailyPnl();
        pnl.setTotalTrades(trades);
        pnl.setTotalPnl(totalPnl);
        pnl.setTradeDate(LocalDate.now(IndianMarketClock.ZONE));
        return pnl;
    }

    private static Long eq(Long value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
