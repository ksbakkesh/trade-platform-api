package com.tradingplatform.repository;

import com.tradingplatform.domain.SystemLog;
import com.tradingplatform.domain.enums.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    Page<SystemLog> findByLevel(LogLevel level, Pageable pageable);
}
