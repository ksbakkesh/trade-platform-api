package com.tradingplatform.angelone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponseData {

    private String jwtToken;
    private String refreshToken;
    private String feedToken;

    @JsonProperty("state")
    private String state;

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getFeedToken() {
        return feedToken;
    }

    public void setFeedToken(String feedToken) {
        this.feedToken = feedToken;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
