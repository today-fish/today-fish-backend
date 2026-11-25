package com.todayfish.backend.collection.publicapi.exception;

public class KmiProviderException extends IllegalStateException {
    private final String resultCode;

    public KmiProviderException(String resultCode) {
        super("KMI Provider API failed: resultCode=" + resultCode);
        this.resultCode = resultCode;
    }

    public String resultCode() {
        return this.resultCode;
    }
}
