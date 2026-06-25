package com.tradingplatform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_pnl", uniqueConstraints = {
        @UniqueConstraint(name = "uq_daily_pnl_account_date", columnNames = {"broker_account_id", "trade_date"})
})
public class DailyPnl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_account_id", nullable = false)
    private BrokerAccount brokerAccount;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "total_trades", nullable = false)
    private Integer totalTrades = 0;

    @Column(name = "total_pnl", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPnl = BigDecimal.ZERO;

    @Column(name = "daily_loss_limit_hit", nullable = false)
    private boolean dailyLossLimitHit = false;

    @Column(name = "max_trades_hit", nullable = false)
    private boolean maxTradesHit = false;

    @Column(name = "trading_disabled", nullable = false)
    private boolean tradingDisabled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BrokerAccount getBrokerAccount() { return brokerAccount; }
    public void setBrokerAccount(BrokerAccount brokerAccount) { this.brokerAccount = brokerAccount; }
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    public Integer getTotalTrades() { return totalTrades; }
    public void setTotalTrades(Integer totalTrades) { this.totalTrades = totalTrades; }
    public BigDecimal getTotalPnl() { return totalPnl; }
    public void setTotalPnl(BigDecimal totalPnl) { this.totalPnl = totalPnl; }
    public boolean isDailyLossLimitHit() { return dailyLossLimitHit; }
    public void setDailyLossLimitHit(boolean dailyLossLimitHit) { this.dailyLossLimitHit = dailyLossLimitHit; }
    public boolean isMaxTradesHit() { return maxTradesHit; }
    public void setMaxTradesHit(boolean maxTradesHit) { this.maxTradesHit = maxTradesHit; }
    public boolean isTradingDisabled() { return tradingDisabled; }
    public void setTradingDisabled(boolean tradingDisabled) { this.tradingDisabled = tradingDisabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
