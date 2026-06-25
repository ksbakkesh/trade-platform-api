package com.tradingplatform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "risk_settings")
public class RiskSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_account_id", nullable = false, unique = true)
    private BrokerAccount brokerAccount;

    @Column(name = "max_trades_per_day", nullable = false)
    private Integer maxTradesPerDay = 2;

    @Column(name = "daily_loss_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyLossLimit = new BigDecimal("4500");

    @Column(nullable = false, length = 30)
    private String scope = "COMBINED";

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
    public Integer getMaxTradesPerDay() { return maxTradesPerDay; }
    public void setMaxTradesPerDay(Integer maxTradesPerDay) { this.maxTradesPerDay = maxTradesPerDay; }
    public BigDecimal getDailyLossLimit() { return dailyLossLimit; }
    public void setDailyLossLimit(BigDecimal dailyLossLimit) { this.dailyLossLimit = dailyLossLimit; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
