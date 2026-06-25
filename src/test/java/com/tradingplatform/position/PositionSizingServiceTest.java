package com.tradingplatform.position;

import com.tradingplatform.angelone.AngelOneMarketClient;
import com.tradingplatform.angelone.dto.FundsResponse;
import com.tradingplatform.domain.BrokerAccount;
import com.tradingplatform.domain.FundManagement;
import com.tradingplatform.domain.StrategySettings;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.domain.enums.QuantityMode;
import com.tradingplatform.repository.FundManagementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionSizingServiceTest {

    @Mock
    private AngelOneMarketClient marketClient;

    @Mock
    private FundManagementRepository fundManagementRepository;

    @InjectMocks
    private PositionSizingService service;

    private BrokerAccount account;

    @BeforeEach
    void setup() {
        account = new BrokerAccount();
        account.setId(1L);
    }

    // -------------------------------------------------------------------------
    // Mode 1 — FIXED_LOTS
    // -------------------------------------------------------------------------

    @Test
    void fixedLots_nifty_2lots() {
        StrategySettings settings = settingsFor(QuantityMode.FIXED_LOTS);
        settings.setFixedLots(2);

        PositionSize result = service.calculate(settings, IndexName.NIFTY,
                new BigDecimal("125"), null);

        assertEquals(QuantityMode.FIXED_LOTS, result.quantityMode());
        assertEquals(75, result.lotSize());
        assertEquals(2, result.numberOfLots());
        assertEquals(150, result.totalQuantity());           // 2 × 75
        assertEquals(new BigDecimal("18750"), result.estimatedCost()); // 150 × 125
        assertTrue(result.isValid());
    }

    @Test
    void fixedLots_sensex_3lots() {
        StrategySettings settings = settingsFor(QuantityMode.FIXED_LOTS);
        settings.setFixedLots(3);

        PositionSize result = service.calculate(settings, IndexName.SENSEX,
                new BigDecimal("200"), null);

        assertEquals(20, result.lotSize());
        assertEquals(3, result.numberOfLots());
        assertEquals(60, result.totalQuantity());            // 3 × 20
        assertEquals(new BigDecimal("12000"), result.estimatedCost()); // 60 × 200
    }

    @Test
    void fixedLots_throwsWhenLotsNotSet() {
        StrategySettings settings = settingsFor(QuantityMode.FIXED_LOTS);
        settings.setFixedLots(null);

        assertThrows(IllegalStateException.class,
                () -> service.calculate(settings, IndexName.NIFTY, new BigDecimal("100"), null));
    }

    // -------------------------------------------------------------------------
    // Mode 2 — CAPITAL_BASED (BRD Section 10 exact example)
    // -------------------------------------------------------------------------

    @Test
    void capitalBased_brdExample_nifty() {
        // BRD example: ₹1,00,000 capital, 20% allocation, ₹125 premium, lot 75
        // → Capital per trade = ₹20,000
        // → Cost per lot = ₹9,375
        // → Calculated lots = 2 (floor of 20000/9375 = 2.13)
        // → Order quantity = 150
        StrategySettings settings = settingsFor(QuantityMode.CAPITAL_BASED);
        settings.setCapitalAllocationPercent(new BigDecimal("20"));

        PositionSize result = service.calculate(settings, IndexName.NIFTY,
                new BigDecimal("125"), new BigDecimal("100000"));

        assertEquals(2, result.numberOfLots());
        assertEquals(150, result.totalQuantity());
        assertEquals(new BigDecimal("18750"), result.estimatedCost());
    }

    @Test
    void capitalBased_sensex_30percentAllocation() {
        // ₹50,000 × 30% = ₹15,000 per trade
        // Cost per lot = ₹250 × 20 = ₹5,000
        // Lots = floor(15000 / 5000) = 3 → qty = 60
        StrategySettings settings = settingsFor(QuantityMode.CAPITAL_BASED);
        settings.setCapitalAllocationPercent(new BigDecimal("30"));

        PositionSize result = service.calculate(settings, IndexName.SENSEX,
                new BigDecimal("250"), new BigDecimal("50000"));

        assertEquals(3, result.numberOfLots());
        assertEquals(60, result.totalQuantity());
    }

    @Test
    void capitalBased_capsAtMaxLots() {
        // Would normally get 2 lots but maxLots=1 caps it
        StrategySettings settings = settingsFor(QuantityMode.CAPITAL_BASED);
        settings.setCapitalAllocationPercent(new BigDecimal("20"));
        settings.setMaxLots(1);

        PositionSize result = service.calculate(settings, IndexName.NIFTY,
                new BigDecimal("125"), new BigDecimal("100000"));

        assertEquals(1, result.numberOfLots());
        assertEquals(75, result.totalQuantity());
    }

    @Test
    void capitalBased_floorsToZeroWhenCapitalTooLow() {
        // ₹500 capital × 10% = ₹50 per trade; cost per lot = ₹9,375 → 0 lots
        StrategySettings settings = settingsFor(QuantityMode.CAPITAL_BASED);
        settings.setCapitalAllocationPercent(new BigDecimal("10"));

        PositionSize result = service.calculate(settings, IndexName.NIFTY,
                new BigDecimal("125"), new BigDecimal("500"));

        assertEquals(0, result.numberOfLots());
        assertEquals(0, result.totalQuantity());
        assertFalse(result.isValid());
    }

    @Test
    void capitalBased_throwsWhenAllocationNotSet() {
        StrategySettings settings = settingsFor(QuantityMode.CAPITAL_BASED);
        settings.setCapitalAllocationPercent(null);

        assertThrows(IllegalStateException.class,
                () -> service.calculate(settings, IndexName.NIFTY,
                        new BigDecimal("125"), new BigDecimal("100000")));
    }

    @Test
    void capitalBased_throwsWhenCapitalNull() {
        StrategySettings settings = settingsFor(QuantityMode.CAPITAL_BASED);
        settings.setCapitalAllocationPercent(new BigDecimal("20"));

        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(settings, IndexName.NIFTY, new BigDecimal("125"), null));
    }

    // -------------------------------------------------------------------------
    // Mode 3 — FIXED_QUANTITY
    // -------------------------------------------------------------------------

    @Test
    void fixedQuantity_exactQuantity() {
        StrategySettings settings = settingsFor(QuantityMode.FIXED_QUANTITY);
        settings.setFixedQuantity(150);

        PositionSize result = service.calculate(settings, IndexName.NIFTY,
                new BigDecimal("125"), null);

        assertEquals(QuantityMode.FIXED_QUANTITY, result.quantityMode());
        assertEquals(150, result.totalQuantity());
        assertEquals(new BigDecimal("18750"), result.estimatedCost()); // 150 × 125
        assertTrue(result.isValid());
    }

    @Test
    void fixedQuantity_throwsWhenQuantityNotSet() {
        StrategySettings settings = settingsFor(QuantityMode.FIXED_QUANTITY);
        settings.setFixedQuantity(null);

        assertThrows(IllegalStateException.class,
                () -> service.calculate(settings, IndexName.NIFTY, new BigDecimal("100"), null));
    }

    // -------------------------------------------------------------------------
    // Fund validation (BRD Section 11)
    // -------------------------------------------------------------------------

    @Test
    void fundValidation_passesWhenSufficientMargin() {
        mockFunds("50000", "40000");
        when(fundManagementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StrategySettings settings = settingsFor(QuantityMode.FIXED_LOTS);
        settings.setFixedLots(2);
        PositionSize size = service.calculate(settings, IndexName.NIFTY,
                new BigDecimal("125"), null); // cost = ₹18,750

        FundValidationResult result = service.validateFunds(account, size);

        assertTrue(result.sufficient());
        assertEquals(new BigDecimal("18750"), result.requiredMargin());
    }

    @Test
    void fundValidation_failsWhenInsufficientMargin() {
        mockFunds("5000", "5000"); // only ₹5,000 available, need ₹18,750
        when(fundManagementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StrategySettings settings = settingsFor(QuantityMode.FIXED_LOTS);
        settings.setFixedLots(2);
        PositionSize size = service.calculate(settings, IndexName.NIFTY,
                new BigDecimal("125"), null);

        FundValidationResult result = service.validateFunds(account, size);

        assertFalse(result.sufficient());
        assertTrue(result.reason().contains("Insufficient"));
    }

    @Test
    void fundValidation_fallsBackToAvailableCashWhenMarginNull() {
        mockFunds("50000", null); // no intraday margin, falls back to available cash
        when(fundManagementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StrategySettings settings = settingsFor(QuantityMode.FIXED_LOTS);
        settings.setFixedLots(2);
        PositionSize size = service.calculate(settings, IndexName.NIFTY,
                new BigDecimal("125"), null);

        FundValidationResult result = service.validateFunds(account, size);

        assertTrue(result.sufficient()); // ₹50,000 cash > ₹18,750 cost
    }

    // -------------------------------------------------------------------------
    // Lot sizes
    // -------------------------------------------------------------------------

    @Test
    void lotSize_niftyIs75() {
        assertEquals(75, IndexLotSize.forIndex(IndexName.NIFTY));
    }

    @Test
    void lotSize_sensexIs20() {
        assertEquals(20, IndexLotSize.forIndex(IndexName.SENSEX));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private StrategySettings settingsFor(QuantityMode mode) {
        StrategySettings s = new StrategySettings();
        s.setQuantityMode(mode);
        return s;
    }

    private void mockFunds(String availableCash, String availableMargin) {
        FundsResponse funds = new FundsResponse();
        funds.setAvailableCash(availableCash);
        funds.setAvailableIntradayPayin(availableMargin);
        when(marketClient.getFunds()).thenReturn(funds);
    }
}
