package com.tradingplatform.angelone.dto;

import java.util.List;

public class QuoteResponse {

    private List<QuoteItem> fetched;
    private List<Object> unfetched;

    public List<QuoteItem> getFetched() {
        return fetched;
    }

    public void setFetched(List<QuoteItem> fetched) {
        this.fetched = fetched;
    }

    public List<Object> getUnfetched() {
        return unfetched;
    }

    public void setUnfetched(List<Object> unfetched) {
        this.unfetched = unfetched;
    }

    public static class QuoteItem {
        private String exchange;
        private String tradingSymbol;
        private String symbolToken;
        private Double ltp;
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Long tradeVolume;

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getTradingSymbol() {
            return tradingSymbol;
        }

        public void setTradingSymbol(String tradingSymbol) {
            this.tradingSymbol = tradingSymbol;
        }

        public String getSymbolToken() {
            return symbolToken;
        }

        public void setSymbolToken(String symbolToken) {
            this.symbolToken = symbolToken;
        }

        public Double getLtp() {
            return ltp;
        }

        public void setLtp(Double ltp) {
            this.ltp = ltp;
        }

        public Double getOpen() {
            return open;
        }

        public void setOpen(Double open) {
            this.open = open;
        }

        public Double getHigh() {
            return high;
        }

        public void setHigh(Double high) {
            this.high = high;
        }

        public Double getLow() {
            return low;
        }

        public void setLow(Double low) {
            this.low = low;
        }

        public Double getClose() {
            return close;
        }

        public void setClose(Double close) {
            this.close = close;
        }

        public Long getTradeVolume() {
            return tradeVolume;
        }

        public void setTradeVolume(Long tradeVolume) {
            this.tradeVolume = tradeVolume;
        }
    }
}
