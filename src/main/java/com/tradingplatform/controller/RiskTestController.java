package com.tradingplatform.controller;

import com.tradingplatform.risk.RiskCheckResult;
import com.tradingplatform.risk.RiskManagementService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Manual verification endpoints for the risk management module.
 *
 * Example:
 *   curl "http://localhost:8080/api/test/risk/summary?accountId=1"
 *   curl -X POST "http://localhost:8080/api/test/risk/record?accountId=1&pnl=-1500"
 *   curl -X POST "http://localhost:8080/api/test/risk/record?accountId=1&pnl=-3200"
 *   curl "http://localhost:8080/api/test/risk/summary?accountId=1"
 */
@RestController
public class RiskTestController {

    private final RiskManagementService riskManagementService;

    public RiskTestController(RiskManagementService riskManagementService) {
        this.riskManagementService = riskManagementService;
    }

    @GetMapping("/api/test/risk/check")
    public RiskCheckResult check(@RequestParam Long accountId) {
        return riskManagementService.checkCanTrade(accountId);
    }

    @GetMapping("/api/test/risk/summary")
    public RiskCheckResult summary(@RequestParam Long accountId) {
        return riskManagementService.getDailyRiskSummary(accountId);
    }

    @PostMapping("/api/test/risk/record")
    public RiskCheckResult record(@RequestParam Long accountId, @RequestParam BigDecimal pnl) {
        riskManagementService.recordTradeResult(accountId, pnl);
        return riskManagementService.getDailyRiskSummary(accountId);
    }
}
