package com.tradingplatform.domain;

import com.tradingplatform.domain.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "signals")
public class Signal {

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
    @Column(name = "signal_type", nullable = false, length = 2)
    private OptionType signalType;

    @Column(name = "open_price", precision = 10, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "buy_above", precision = 10, scale = 2)
    private BigDecimal buyAbove;

    @Column(name = "sell_below", precision = 10, scale = 2)
    private BigDecimal sellBelow;

    @Column(name = "strike_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal strikePrice;

    @Column(name = "trading_symbol", length = 50)
    private String tradingSymbol;

    @Column(name = "symbol_token", length = 20)
    private String symbolToken;

    @Column(name = "premium_at_signal", precision = 10, scale = 2)
    private BigDecimal premiumAtSignal;

    @Column(name = "rsi_value", precision = 5, scale = 2)
    private BigDecimal rsiValue;

    @Column(name = "volume_ratio", precision = 6, scale = 3)
    private BigDecimal volumeRatio;

    @Column(name = "delta_value", precision = 4, scale = 3)
    private BigDecimal deltaValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SignalStatus status = SignalStatus.GENERATED;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @PrePersist
    void onCreate() { if (generatedAt == null) generatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    @JsonIgnore
    public BrokerAccount getBrokerAccount() { return brokerAccount; }
    public void setBrokerAccount(BrokerAccount brokerAccount) { this.brokerAccount = brokerAccount; }
    public IndexName getIndexName() { return indexName; }
    public void setIndexName(IndexName indexName) { this.indexName = indexName; }
    public OptionType getSignalType() { return signalType; }
    public void setSignalType(OptionType signalType) { this.signalType = signalType; }
    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }
    public BigDecimal getBuyAbove() { return buyAbove; }
    public void setBuyAbove(BigDecimal buyAbove) { this.buyAbove = buyAbove; }
    public BigDecimal getSellBelow() { return sellBelow; }
    public void setSellBelow(BigDecimal sellBelow) { this.sellBelow = sellBelow; }
    public BigDecimal getStrikePrice() { return strikePrice; }
    public void setStrikePrice(BigDecimal strikePrice) { this.strikePrice = strikePrice; }
    public String getTradingSymbol() { return tradingSymbol; }
    public void setTradingSymbol(String tradingSymbol) { this.tradingSymbol = tradingSymbol; }
    public String getSymbolToken() { return symbolToken; }
    public void setSymbolToken(String symbolToken) { this.symbolToken = symbolToken; }
    public BigDecimal getPremiumAtSignal() { return premiumAtSignal; }
    public void setPremiumAtSignal(BigDecimal premiumAtSignal) { this.premiumAtSignal = premiumAtSignal; }
    public BigDecimal getRsiValue() { return rsiValue; }
    public void setRsiValue(BigDecimal rsiValue) { this.rsiValue = rsiValue; }
    public BigDecimal getVolumeRatio() { return volumeRatio; }
    public void setVolumeRatio(BigDecimal volumeRatio) { this.volumeRatio = volumeRatio; }
    public BigDecimal getDeltaValue() { return deltaValue; }
    public void setDeltaValue(BigDecimal deltaValue) { this.deltaValue = deltaValue; }
    public SignalStatus getStatus() { return status; }
    public void setStatus(SignalStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
