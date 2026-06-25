package com.tradingplatform.angelone;

/**
 * Thrown when Angel One returns status=false, an HTTP error, or the response
 * shape doesn't match what we expect. Carries the broker's own error code/message
 * so callers (and logs) can tell exactly what the broker rejected and why.
 */
public class AngelOneApiException extends RuntimeException {

    private final String errorCode;

    public AngelOneApiException(String message, String errorCode) {
        super(message + " (errorcode=" + errorCode + ")");
        this.errorCode = errorCode;
    }

    public AngelOneApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
