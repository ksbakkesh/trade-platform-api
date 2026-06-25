package com.tradingplatform.domain.enums;

/**
 * Supported broker integrations.
 * Only ANGEL_ONE is fully implemented. Others are stubs ready for future development.
 */
public enum BrokerType {
    ANGEL_ONE("Angel One", "SmartAPI", true,  "https://smartapi.angelone.in"),
    ZERODHA(  "Zerodha",   "Kite API",  false, "https://kite.trade/docs/connect/v3/"),
    UPSTOX(   "Upstox",    "Upstox v3", false, "https://upstox.com/developer/api-documentation/"),
    DHAN(     "Dhan",      "DhanHQ",    false, "https://dhanhq.co/docs/v2/");

    private final String displayName;
    private final String apiName;
    private final boolean implemented;
    private final String docsUrl;

    BrokerType(String displayName, String apiName, boolean implemented, String docsUrl) {
        this.displayName = displayName;
        this.apiName = apiName;
        this.implemented = implemented;
        this.docsUrl = docsUrl;
    }

    public String getDisplayName()  { return displayName; }
    public String getApiName()      { return apiName; }
    public boolean isImplemented()  { return implemented; }
    public String getDocsUrl()      { return docsUrl; }
}
