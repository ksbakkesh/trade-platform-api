package com.tradingplatform.repository;

import com.tradingplatform.domain.RiskSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiskSettingsRepository extends JpaRepository<RiskSettings, Long> {
    Optional<RiskSettings> findByBrokerAccountId(Long brokerAccountId);
}
