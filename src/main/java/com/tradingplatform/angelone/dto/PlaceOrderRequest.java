package com.tradingplatform.angelone.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for /order/v1/placeOrder.
 * Field names intentionally mirror Angel One's API exactly (lowercase, no camelCase)
 * since Jackson serializes this directly to JSON sent to their servers.
 */
public class PlaceOrderRequest {

    @NotBlank
    private String variety = "NORMAL";       // NORMAL, STOPLOSS, AMO, ROBO

    @NotBlank
    private String tradingsymbol;             // e.g. "NIFTY28NOV2421500CE"

    @NotBlank
    private String symboltoken;

    @NotBlank
    private String transactiontype;            // BUY or SELL

    @NotBlank
    private String exchange = "NFO";

    @NotBlank
    private String ordertype = "MARKET";        // MARKET, LIMIT, STOPLOSS_LIMIT, STOPLOSS_MARKET

    @NotBlank
    private String producttype = "INTRADAY";     // INTRADAY, CARRYFORWARD, MARGIN, DELIVERY

    @NotBlank
    private String duration = "DAY";              // DAY or IOC

    private String price = "0";                     // required for LIMIT orders, "0" for MARKET
    private String squareoff = "0";
    private String stoploss = "0";

    @NotBlank
    private String quantity;

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getTradingsymbol() {
        return tradingsymbol;
    }

    public void setTradingsymbol(String tradingsymbol) {
        this.tradingsymbol = tradingsymbol;
    }

    public String getSymboltoken() {
        return symboltoken;
    }

    public void setSymboltoken(String symboltoken) {
        this.symboltoken = symboltoken;
    }

    public String getTransactiontype() {
        return transactiontype;
    }

    public void setTransactiontype(String transactiontype) {
        this.transactiontype = transactiontype;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getOrdertype() {
        return ordertype;
    }

    public void setOrdertype(String ordertype) {
        this.ordertype = ordertype;
    }

    public String getProducttype() {
        return producttype;
    }

    public void setProducttype(String producttype) {
        this.producttype = producttype;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getSquareoff() {
        return squareoff;
    }

    public void setSquareoff(String squareoff) {
        this.squareoff = squareoff;
    }

    public String getStoploss() {
        return stoploss;
    }

    public void setStoploss(String stoploss) {
        this.stoploss = stoploss;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }
}
