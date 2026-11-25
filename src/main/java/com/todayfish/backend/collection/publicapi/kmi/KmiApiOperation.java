package com.todayfish.backend.collection.publicapi.kmi;

public enum KmiApiOperation {

    PRODUCTION_PRICE("prdlcPrice"),
    WHOLESALE_PRICE("whsalPrice"),
    RETAIL_PRICE("rtlsalPrice");

    private final String subAction;

    KmiApiOperation(String subAction) {
        this.subAction = subAction;
    }

    public String subAction() {
        return subAction;
    }
}