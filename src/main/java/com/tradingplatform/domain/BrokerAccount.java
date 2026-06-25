package com.tradingplatform.domain;

import com.tradingplatform.security.EncryptedStringConverter;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "broker_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_broker_account_client", columnNames = {"user_id", "client_code"})
})
public class BrokerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "broker_name", nullable = false, length = 50)
    private String brokerName = "ANGEL_ONE";

    @Column(name = "client_code", nullable = false, length = 50)
    private String clientCode;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_key_encrypted", nullable = false, length = 500)
    private String apiKey;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "password_encrypted", nullable = false, length = 500)
    private String password;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "totp_secret_encrypted", nullable = false, length = 500)
    private String totpSecret;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

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
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getBrokerName() { return brokerName; }
    public void setBrokerName(String brokerName) { this.brokerName = brokerName; }
    public String getClientCode() { return clientCode; }
    public void setClientCode(String clientCode) { this.clientCode = clientCode; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
