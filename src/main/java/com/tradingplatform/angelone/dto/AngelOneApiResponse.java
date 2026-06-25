package com.tradingplatform.angelone.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Every Angel One SmartAPI response follows this envelope shape.
 * Note: different endpoints use slightly different field names —
 * auth endpoints use "status"/"errorcode", order endpoints use "success"/"errorCode".
 * JsonAlias handles both variants.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AngelOneApiResponse<T> {

    @JsonAlias({"status", "success"})
    private boolean status;

    private String message;

    @JsonAlias({"errorcode", "errorCode"})
    private String errorcode;

    private T data;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorcode() {
        return errorcode;
    }

    public void setErrorcode(String errorcode) {
        this.errorcode = errorcode;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
