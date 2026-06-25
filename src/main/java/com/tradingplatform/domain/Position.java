package com.tradingplatform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_id", nullable = false, unique = true)
    private Trade trade;

    @Column(name = "quantity_remaining", nullable = false)
    private Integer quantityRemaining;

    @Column(name = "current_ltp", precision = 10, scale = 2)
    private BigDecimal currentLtp;

    @Column(name = "current_stop_loss", precision = 10, scale = 2)
    private BigDecimal currentStopLoss;

    @Column(name = "unrealized_pnl", precision = 10, scale = 2)
    private BigDecimal unrealizedPnl;

    @Column(name = "sl_moved_to_cost", nullable = false)
    private boolean slMovedToCost = false;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @PrePersist
    @PreUpdate
    void touch() { lastUpdatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Trade getTrade() { return trade; }
    public void setTrade(Trade trade) { this.trade = trade; }
    public Integer getQuantityRemaining() { return quantityRemaining; }
    public void setQuantityRemaining(Integer quantityRemaining) { this.quantityRemaining = quantityRemaining; }
    public BigDecimal getCurrentLtp() { return currentLtp; }
    public void setCurrentLtp(BigDecimal currentLtp) { this.currentLtp = currentLtp; }
    public BigDecimal getCurrentStopLoss() { return currentStopLoss; }
    public void setCurrentStopLoss(BigDecimal currentStopLoss) { this.currentStopLoss = currentStopLoss; }
    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public void setUnrealizedPnl(BigDecimal unrealizedPnl) { this.unrealizedPnl = unrealizedPnl; }
    public boolean isSlMovedToCost() { return slMovedToCost; }
    public void setSlMovedToCost(boolean slMovedToCost) { this.slMovedToCost = slMovedToCost; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
}
