package com.tradingplatform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fund_management")
public class FundManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_account_id", nullable = false)
    private BrokerAccount brokerAccount;

    @Column(name = "available_funds", precision = 12, scale = 2)
    private BigDecimal availableFunds;

    @Column(name = "available_margin", precision = 12, scale = 2)
    private BigDecimal availableMargin;

    @Column(name = "used_margin", precision = 12, scale = 2)
    private BigDecimal usedMargin;

    @Column(name = "today_pnl", precision = 12, scale = 2)
    private BigDecimal todayPnl;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @PrePersist
    void onCreate() { if (fetchedAt == null) fetchedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BrokerAccount getBrokerAccount() { return brokerAccount; }
    public void setBrokerAccount(BrokerAccount brokerAccount) { this.brokerAccount = brokerAccount; }
    public BigDecimal getAvailableFunds() { return availableFunds; }
    public void setAvailableFunds(BigDecimal availableFunds) { this.availableFunds = availableFunds; }
    public BigDecimal getAvailableMargin() { return availableMargin; }
    public void setAvailableMargin(BigDecimal availableMargin) { this.availableMargin = availableMargin; }
    public BigDecimal getUsedMargin() { return usedMargin; }
    public void setUsedMargin(BigDecimal usedMargin) { this.usedMargin = usedMargin; }
    public BigDecimal getTodayPnl() { return todayPnl; }
    public void setTodayPnl(BigDecimal todayPnl) { this.todayPnl = todayPnl; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
}
