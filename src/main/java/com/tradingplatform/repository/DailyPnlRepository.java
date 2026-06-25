package com.tradingplatform.repository;

import com.tradingplatform.domain.DailyPnl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyPnlRepository extends JpaRepository<DailyPnl, Long> {
    Optional<DailyPnl> findByBrokerAccountIdAndTradeDate(Long brokerAccountId, LocalDate tradeDate);
}
