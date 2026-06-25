package com.tradingplatform.domain;

import com.tradingplatform.domain.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_id")
    private Signal signal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "broker_account_id", nullable = false)
    private BrokerAccount brokerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "index_name", nullable = false, length = 10)
    private IndexName indexName;

    @Column(name = "trading_symbol", nullable = false, length = 50)
    private String tradingSymbol;

    @Column(name = "symbol_token", nullable = false, length = 20)
    private String symbolToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 4)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "entry_price", precision = 10, scale = 2)
    private BigDecimal entryPrice;

    @Column(name = "stop_loss_price", precision = 10, scale = 2)
    private BigDecimal stopLossPrice;

    @Column(name = "target1_price", precision = 10, scale = 2)
    private BigDecimal target1Price;

    @Column(name = "target2_price", precision = 10, scale = 2)
    private BigDecimal target2Price;

    @Column(name = "exit_price", precision = 10, scale = 2)
    private BigDecimal exitPrice;

    @Column(name = "broker_order_id", length = 50)
    private String brokerOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status = TradeStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "exit_reason", length = 30)
    private ExitReason exitReason;

    @Column(name = "realized_pnl", precision = 10, scale = 2)
    private BigDecimal realizedPnl;

    @Column(name = "is_reentry", nullable = false)
    private boolean reentry = false;

    @Column(name = "entry_time")
    private Instant entryTime;

    @Column(name = "exit_time")
    private Instant exitTime;

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
    public Signal getSignal() { return signal; }
    public void setSignal(Signal signal) { this.signal = signal; }
    public BrokerAccount getBrokerAccount() { return brokerAccount; }
    public void setBrokerAccount(BrokerAccount brokerAccount) { this.brokerAccount = brokerAccount; }
    public IndexName getIndexName() { return indexName; }
    public void setIndexName(IndexName indexName) { this.indexName = indexName; }
    public String getTradingSymbol() { return tradingSymbol; }
    public void setTradingSymbol(String tradingSymbol) { this.tradingSymbol = tradingSymbol; }
    public String getSymbolToken() { return symbolToken; }
    public void setSymbolToken(String symbolToken) { this.symbolToken = symbolToken; }
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public BigDecimal getStopLossPrice() { return stopLossPrice; }
    public void setStopLossPrice(BigDecimal stopLossPrice) { this.stopLossPrice = stopLossPrice; }
    public BigDecimal getTarget1Price() { return target1Price; }
    public void setTarget1Price(BigDecimal target1Price) { this.target1Price = target1Price; }
    public BigDecimal getTarget2Price() { return target2Price; }
    public void setTarget2Price(BigDecimal target2Price) { this.target2Price = target2Price; }
    public BigDecimal getExitPrice() { return exitPrice; }
    public void setExitPrice(BigDecimal exitPrice) { this.exitPrice = exitPrice; }
    public String getBrokerOrderId() { return brokerOrderId; }
    public void setBrokerOrderId(String brokerOrderId) { this.brokerOrderId = brokerOrderId; }
    public TradeStatus getStatus() { return status; }
    public void setStatus(TradeStatus status) { this.status = status; }
    public ExitReason getExitReason() { return exitReason; }
    public void setExitReason(ExitReason exitReason) { this.exitReason = exitReason; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }
    public boolean isReentry() { return reentry; }
    public void setReentry(boolean reentry) { this.reentry = reentry; }
    public Instant getEntryTime() { return entryTime; }
    public void setEntryTime(Instant entryTime) { this.entryTime = entryTime; }
    public Instant getExitTime() { return exitTime; }
    public void setExitTime(Instant exitTime) { this.exitTime = exitTime; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
