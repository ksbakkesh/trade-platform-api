package com.tradingplatform.repository;

import com.tradingplatform.domain.Signal;
import com.tradingplatform.domain.enums.SignalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SignalRepository extends JpaRepository<Signal, Long> {
    List<Signal> findByBrokerAccountIdAndStatus(Long brokerAccountId, SignalStatus status);
    List<Signal> findByBrokerAccountIdAndGeneratedAtBetween(Long brokerAccountId, Instant from, Instant to);
}
