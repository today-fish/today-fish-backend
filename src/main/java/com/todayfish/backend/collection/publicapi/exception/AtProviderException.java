package com.todayfish.backend.collection.publicapi.exception;

public class AtProviderException extends IllegalStateException {

    private final String resultCode;

    public AtProviderException(String resultCode) {
        super("aT Provider API failed: resultCode= " + resultCode);
        this.resultCode = resultCode;
    }

    public String resultCode() {
        return this.resultCode;
    }
}
