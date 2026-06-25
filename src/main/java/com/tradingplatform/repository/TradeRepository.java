package com.tradingplatform.repository;

import com.tradingplatform.domain.Trade;
import com.tradingplatform.domain.enums.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByBrokerAccountIdAndStatus(Long brokerAccountId, TradeStatus status);
    List<Trade> findByBrokerAccountIdAndEntryTimeBetween(Long brokerAccountId, Instant from, Instant to);
    long countByBrokerAccountIdAndEntryTimeBetween(Long brokerAccountId, Instant from, Instant to);
}
