package com.tradingplatform.angelone.dto;

import java.util.List;
import java.util.Map;

/**
 * Body for the /market/v1/quote/ endpoint.
 * Example: { "mode": "FULL", "exchangeTokens": { "NSE": ["3045"] } }
 */
public class QuoteRequest {

    private String mode;
    private Map<String, List<String>> exchangeTokens;

    public QuoteRequest(String mode, Map<String, List<String>> exchangeTokens) {
        this.mode = mode;
        this.exchangeTokens = exchangeTokens;
    }

    public String getMode() {
        return mode;
    }

    public Map<String, List<String>> getExchangeTokens() {
        return exchangeTokens;
    }
}
