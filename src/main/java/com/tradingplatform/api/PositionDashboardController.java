package com.tradingplatform.api;

import com.tradingplatform.domain.Position;
import com.tradingplatform.repository.PositionRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Dashboard — live open positions with current LTP, unrealized P&L, SL status.
 */
@RestController
@RequestMapping("/api/dashboard/positions")
public class PositionDashboardController {

    private final PositionRepository positionRepository;

    public PositionDashboardController(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @GetMapping
    public List<PositionSummary> openPositions(@RequestParam Long accountId) {
        return positionRepository.findByTrade_BrokerAccount_Id(accountId)
                .stream()
                .filter(p -> p.getQuantityRemaining() > 0)
                .map(PositionSummary::from)
                .toList();
    }

    public record PositionSummary(
            Long positionId,
            Long tradeId,
            String tradingSymbol,
            Integer quantityRemaining,
            BigDecimal currentLtp,
            BigDecimal currentStopLoss,
            BigDecimal unrealizedPnl,
            boolean slMovedToCost,
            Instant lastUpdatedAt
    ) {
        public static PositionSummary from(Position p) {
            return new PositionSummary(
                    p.getId(),
                    p.getTrade().getId(),
                    p.getTrade().getTradingSymbol(),
                    p.getQuantityRemaining(),
                    p.getCurrentLtp(),
                    p.getCurrentStopLoss(),
                    p.getUnrealizedPnl(),
                    p.isSlMovedToCost(),
                    p.getLastUpdatedAt()
            );
        }
    }
}
