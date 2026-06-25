package com.tradingplatform.domain;

import com.tradingplatform.domain.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "strategy_settings", uniqueConstraints = {
        @UniqueConstraint(name = "uq_strategy_settings_account_index", columnNames = {"broker_account_id", "index_name"})
})
public class StrategySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_account_id", nullable = false)
    private BrokerAccount brokerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "index_name", nullable = false, length = 10)
    private IndexName indexName;

    @Enumerated(EnumType.STRING)
    @Column(name = "open_price_mode", nullable = false, length = 10)
    private OpenPriceMode openPriceMode = OpenPriceMode.AUTO;

    @Column(name = "manual_open_price", precision = 10, scale = 2)
    private BigDecimal manualOpenPrice;

    @Column(name = "premium_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumThreshold;

    @Column(name = "candle_timeframe_minutes", nullable = false)
    private Integer candleTimeframeMinutes = 15;

    @Column(name = "rsi_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal rsiThreshold = new BigDecimal("60");

    @Column(name = "volume_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal volumeMultiplier = new BigDecimal("2");

    @Column(name = "delta_min", nullable = false, precision = 4, scale = 3)
    private BigDecimal deltaMin = new BigDecimal("0.450");

    @Column(name = "delta_max", nullable = false, precision = 4, scale = 3)
    private BigDecimal deltaMax = new BigDecimal("0.650");

    @Column(name = "stop_loss_points", nullable = false, precision = 10, scale = 2)
    private BigDecimal stopLossPoints;

    @Column(name = "target1_points", nullable = false, precision = 10, scale = 2)
    private BigDecimal target1Points;

    @Column(name = "target2_points", nullable = false, precision = 10, scale = 2)
    private BigDecimal target2Points;

    @Enumerated(EnumType.STRING)
    @Column(name = "exit_strategy_mode", nullable = false, length = 10)
    private ExitStrategyMode exitStrategyMode = ExitStrategyMode.OPTION1;

    @Column(name = "re_entry_enabled", nullable = false)
    private boolean reEntryEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_mode", nullable = false, length = 20)
    private QuantityMode quantityMode = QuantityMode.CAPITAL_BASED;

    @Column(name = "fixed_lots")
    private Integer fixedLots;

    @Column(name = "fixed_quantity")
    private Integer fixedQuantity;

    @Column(name = "capital_allocation_percent", precision = 5, scale = 2)
    private BigDecimal capitalAllocationPercent;

    @Column(name = "max_lots")
    private Integer maxLots;

    @Column(name = "auto_trading_enabled", nullable = false)
    private boolean autoTradingEnabled = false;

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
    public IndexName getIndexName() { return indexName; }
    public void setIndexName(IndexName indexName) { this.indexName = indexName; }
    public OpenPriceMode getOpenPriceMode() { return openPriceMode; }
    public void setOpenPriceMode(OpenPriceMode openPriceMode) { this.openPriceMode = openPriceMode; }
    public BigDecimal getManualOpenPrice() { return manualOpenPrice; }
    public void setManualOpenPrice(BigDecimal manualOpenPrice) { this.manualOpenPrice = manualOpenPrice; }
    public BigDecimal getPremiumThreshold() { return premiumThreshold; }
    public void setPremiumThreshold(BigDecimal premiumThreshold) { this.premiumThreshold = premiumThreshold; }
    public Integer getCandleTimeframeMinutes() { return candleTimeframeMinutes; }
    public void setCandleTimeframeMinutes(Integer candleTimeframeMinutes) { this.candleTimeframeMinutes = candleTimeframeMinutes; }
    public BigDecimal getRsiThreshold() { return rsiThreshold; }
    public void setRsiThreshold(BigDecimal rsiThreshold) { this.rsiThreshold = rsiThreshold; }
    public BigDecimal getVolumeMultiplier() { return volumeMultiplier; }
    public void setVolumeMultiplier(BigDecimal volumeMultiplier) { this.volumeMultiplier = volumeMultiplier; }
    public BigDecimal getDeltaMin() { return deltaMin; }
    public void setDeltaMin(BigDecimal deltaMin) { this.deltaMin = deltaMin; }
    public BigDecimal getDeltaMax() { return deltaMax; }
    public void setDeltaMax(BigDecimal deltaMax) { this.deltaMax = deltaMax; }
    public BigDecimal getStopLossPoints() { return stopLossPoints; }
    public void setStopLossPoints(BigDecimal stopLossPoints) { this.stopLossPoints = stopLossPoints; }
    public BigDecimal getTarget1Points() { return target1Points; }
    public void setTarget1Points(BigDecimal target1Points) { this.target1Points = target1Points; }
    public BigDecimal getTarget2Points() { return target2Points; }
    public void setTarget2Points(BigDecimal target2Points) { this.target2Points = target2Points; }
    public ExitStrategyMode getExitStrategyMode() { return exitStrategyMode; }
    public void setExitStrategyMode(ExitStrategyMode exitStrategyMode) { this.exitStrategyMode = exitStrategyMode; }
    public boolean isReEntryEnabled() { return reEntryEnabled; }
    public void setReEntryEnabled(boolean reEntryEnabled) { this.reEntryEnabled = reEntryEnabled; }
    public QuantityMode getQuantityMode() { return quantityMode; }
    public void setQuantityMode(QuantityMode quantityMode) { this.quantityMode = quantityMode; }
    public Integer getFixedLots() { return fixedLots; }
    public void setFixedLots(Integer fixedLots) { this.fixedLots = fixedLots; }
    public Integer getFixedQuantity() { return fixedQuantity; }
    public void setFixedQuantity(Integer fixedQuantity) { this.fixedQuantity = fixedQuantity; }
    public BigDecimal getCapitalAllocationPercent() { return capitalAllocationPercent; }
    public void setCapitalAllocationPercent(BigDecimal capitalAllocationPercent) { this.capitalAllocationPercent = capitalAllocationPercent; }
    public Integer getMaxLots() { return maxLots; }
    public void setMaxLots(Integer maxLots) { this.maxLots = maxLots; }
    public boolean isAutoTradingEnabled() { return autoTradingEnabled; }
    public void setAutoTradingEnabled(boolean autoTradingEnabled) { this.autoTradingEnabled = autoTradingEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
