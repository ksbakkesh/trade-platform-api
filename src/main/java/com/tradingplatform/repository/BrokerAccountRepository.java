package com.tradingplatform.repository;

import com.tradingplatform.domain.BrokerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrokerAccountRepository extends JpaRepository<BrokerAccount, Long> {
    List<BrokerAccount> findByUserId(Long userId);
    Optional<BrokerAccount> findByUserIdAndClientCode(Long userId, String clientCode);
    List<BrokerAccount> findByActiveTrue();
}
