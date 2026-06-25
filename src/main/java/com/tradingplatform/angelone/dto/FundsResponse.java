package com.tradingplatform.angelone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Angel One getRMS (Risk Management System) response data.
 * Endpoint: GET /rest/secure/angelbroking/user/v1/getRMS
 *
 * Returns the current funds and margin breakdown for the logged-in account.
 */
public class FundsResponse {

    @JsonProperty("availablecash")
    private String availableCash;

    @JsonProperty("availableintradaypayin")
    private String availableIntradayPayin;

    @JsonProperty("availabledelvryamt")
    private String availableDeliveryAmount;

    @JsonProperty("t1holdings")
    private String t1Holdings;

    @JsonProperty("collateral")
    private String collateral;

    @JsonProperty("collateralquantity")
    private String collateralQuantity;

    @JsonProperty("m2mrealized")
    private String m2mRealized;

    @JsonProperty("m2munrealized")
    private String m2mUnrealized;

    @JsonProperty("utiliseddebits")
    private String utilisedDebits;

    @JsonProperty("utilisedspan")
    private String utilisedSpan;

    @JsonProperty("utilisedoptionpremium")
    private String utilisedOptionPremium;

    @JsonProperty("utilisedturnover")
    private String utilisedTurnover;

    @JsonProperty("utilisedpayout")
    private String utilisedPayout;

    @JsonProperty("utilisedholdings")
    private String utilisedHoldings;

    public String getAvailableCash() { return availableCash; }
    public void setAvailableCash(String v) { this.availableCash = v; }

    public String getAvailableIntradayPayin() { return availableIntradayPayin; }
    public void setAvailableIntradayPayin(String v) { this.availableIntradayPayin = v; }

    public String getM2mRealized() { return m2mRealized; }
    public void setM2mRealized(String v) { this.m2mRealized = v; }

    public String getM2mUnrealized() { return m2mUnrealized; }
    public void setM2mUnrealized(String v) { this.m2mUnrealized = v; }

    public String getUtilisedDebits() { return utilisedDebits; }
    public void setUtilisedDebits(String v) { this.utilisedDebits = v; }
}
