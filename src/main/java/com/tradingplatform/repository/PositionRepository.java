package com.tradingplatform.repository;

import com.tradingplatform.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    Optional<Position> findByTradeId(Long tradeId);
    List<Position> findByTrade_BrokerAccount_Id(Long brokerAccountId);
}
