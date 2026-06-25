package com.tradingplatform.api;

import com.tradingplatform.domain.Signal;
import com.tradingplatform.domain.enums.IndexName;
import com.tradingplatform.domain.enums.OptionType;
import com.tradingplatform.domain.enums.SignalStatus;
import com.tradingplatform.repository.SignalRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Dashboard — signal history and today's signals.
 */
@RestController
@RequestMapping("/api/dashboard/signals")
public class SignalDashboardController {

    private final SignalRepository signalRepository;

    public SignalDashboardController(SignalRepository signalRepository) {
        this.signalRepository = signalRepository;
    }

    /** Today's signals for an account */
    @GetMapping("/today")
    public List<SignalSummary> today(@RequestParam Long accountId) {
        Instant startOfDay = LocalDate.now(ZoneId.of("Asia/Kolkata"))
                .atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        return signalRepository
                .findByBrokerAccountIdAndGeneratedAtBetween(accountId, startOfDay, Instant.now())
                .stream().map(SignalSummary::from).toList();
    }

    /** Signals for a date range */
    @GetMapping
    public List<SignalSummary> list(@RequestParam Long accountId,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
        return signalRepository
                .findByBrokerAccountIdAndGeneratedAtBetween(accountId, fromInstant, toInstant)
                .stream().map(SignalSummary::from).toList();
    }

    /** Signals by status */
    @GetMapping("/status/{status}")
    public List<SignalSummary> byStatus(@RequestParam Long accountId,
                                         @PathVariable SignalStatus status) {
        return signalRepository.findByBrokerAccountIdAndStatus(accountId, status)
                .stream().map(SignalSummary::from).toList();
    }

    public record SignalSummary(
            Long id, IndexName indexName, OptionType signalType,
            BigDecimal openPrice, BigDecimal buyAbove, BigDecimal sellBelow,
            BigDecimal strikePrice, String tradingSymbol,
            BigDecimal premiumAtSignal, BigDecimal rsiValue,
            BigDecimal volumeRatio, BigDecimal deltaValue,
            SignalStatus status, String rejectionReason, Instant generatedAt
    ) {
        public static SignalSummary from(Signal s) {
            return new SignalSummary(
                    s.getId(), s.getIndexName(), s.getSignalType(),
                    s.getOpenPrice(), s.getBuyAbove(), s.getSellBelow(),
                    s.getStrikePrice(), s.getTradingSymbol(),
                    s.getPremiumAtSignal(), s.getRsiValue(),
                    s.getVolumeRatio(), s.getDeltaValue(),
                    s.getStatus(), s.getRejectionReason(), s.getGeneratedAt()
            );
        }
    }
}
