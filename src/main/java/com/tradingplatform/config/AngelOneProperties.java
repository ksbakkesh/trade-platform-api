package com.tradingplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Binds the `angelone.*` keys from application.yml.
 * All values are expected to come from environment variables - see application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "angelone")
public class AngelOneProperties {

    @NotBlank(message = "ANGELONE_API_KEY is not set")
    private String apiKey;

    @NotBlank(message = "ANGELONE_CLIENT_CODE is not set")
    private String clientCode;

    @NotBlank(message = "ANGELONE_PASSWORD is not set")
    private String password;

    @NotBlank(message = "ANGELONE_TOTP_SECRET is not set")
    private String totpSecret;

    @NotBlank
    private String baseUrl;

    private String clientLocalIp;
    private String clientPublicIp;
    private String macAddress;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClientLocalIp() {
        return clientLocalIp;
    }

    public void setClientLocalIp(String clientLocalIp) {
        this.clientLocalIp = clientLocalIp;
    }

    public String getClientPublicIp() {
        return clientPublicIp;
    }

    public void setClientPublicIp(String clientPublicIp) {
        this.clientPublicIp = clientPublicIp;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
