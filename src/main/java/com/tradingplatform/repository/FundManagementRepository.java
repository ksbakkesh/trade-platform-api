package com.tradingplatform.repository;

import com.tradingplatform.domain.FundManagement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FundManagementRepository extends JpaRepository<FundManagement, Long> {
    Optional<FundManagement> findFirstByBrokerAccountIdOrderByFetchedAtDesc(Long brokerAccountId);
}
