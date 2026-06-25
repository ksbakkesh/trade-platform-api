package com.tradingplatform.controller;

import com.tradingplatform.domain.Position;
import com.tradingplatform.domain.Trade;
import com.tradingplatform.exit.ExitResult;
import com.tradingplatform.exit.ExitStrategyService;
import com.tradingplatform.repository.PositionRepository;
import com.tradingplatform.repository.TradeRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Simulates LTP updates for an open trade to test exit logic.
 *
 * Example — simulate Target 1 hit:
 *   curl -X POST "http://localhost:8080/api/test/exit/monitor/1?ltp=310"
 *
 * Example — simulate Stop Loss hit:
 *   curl -X POST "http://localhost:8080/api/test/exit/monitor/1?ltp=50"
 *
 * Example — simulate Target 2 hit (after partial close):
 *   curl -X POST "http://localhost:8080/api/test/exit/monitor/1?ltp=350"
 */
@RestController
public class ExitStrategyTestController {

    private final ExitStrategyService exitStrategyService;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;

    public ExitStrategyTestController(ExitStrategyService exitStrategyService,
                                       TradeRepository tradeRepository,
                                       PositionRepository positionRepository) {
        this.exitStrategyService = exitStrategyService;
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
    }

    @PostMapping("/api/test/exit/monitor/{tradeId}")
    public Map<String, Object> monitor(@PathVariable Long tradeId,
                                        @RequestParam BigDecimal ltp) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("No trade found: " + tradeId));

        Position position = positionRepository.findByTradeId(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("No position found for trade: " + tradeId));

        ExitResult result = exitStrategyService.monitor(trade, position, ltp);

        return Map.of(
                "action", result.actionTaken().name(),
                "trigger", result.trigger().name(),
                "exitReason", result.exitReason() != null ? result.exitReason().name() : "NONE",
                "exitPrice", result.exitPrice(),
                "quantityClosed", result.quantityClosed(),
                "realizedPnl", result.realizedPnl() != null ? result.realizedPnl() : 0,
                "tradeStatus", result.trade() != null ? result.trade().getStatus().name() : "UNCHANGED"
        );
    }
}
