package com.tradingplatform.angelone.dto;

public class OrderResponse {

    private String script;
    private String orderid;
    private String uniqueorderid;

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getOrderid() {
        return orderid;
    }

    public void setOrderid(String orderid) {
        this.orderid = orderid;
    }

    public String getUniqueorderid() {
        return uniqueorderid;
    }

    public void setUniqueorderid(String uniqueorderid) {
        this.uniqueorderid = uniqueorderid;
    }
}
