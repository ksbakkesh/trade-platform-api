package com.tradingplatform.repository;

import com.tradingplatform.domain.StrategySettings;
import com.tradingplatform.domain.enums.IndexName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StrategySettingsRepository extends JpaRepository<StrategySettings, Long> {
    Optional<StrategySettings> findByBrokerAccountIdAndIndexName(Long brokerAccountId, IndexName indexName);
}
