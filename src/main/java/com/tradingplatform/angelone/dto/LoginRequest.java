package com.tradingplatform.angelone.dto;

public class LoginRequest {

    private String clientcode;
    private String password;
    private String totp;

    public LoginRequest(String clientcode, String password, String totp) {
        this.clientcode = clientcode;
        this.password = password;
        this.totp = totp;
    }

    public String getClientcode() {
        return clientcode;
    }

    public String getPassword() {
        return password;
    }

    public String getTotp() {
        return totp;
    }
}
