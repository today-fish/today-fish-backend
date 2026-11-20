package com.todayfish.backend.collection.publicapi.exception;

import com.todayfish.backend.collection.publicapi.model.CollectionRequestCommand;
import com.todayfish.backend.collection.publicapi.model.RawApiResponse;

public class CollectionResponseException extends RuntimeException {

    public enum FailureType {
        HTTP_ERROR, // HTTP 수준 오류
        PROVIDER_ERROR, // HTTP는 서옹했지만 제공기관이 실패 응답
        INVALID_RESPONSE // 응답 형식이나 구조가 잘못됨
    }

    private final FailureType failureType;
    private final RawApiResponse rawApiResponse;

    public CollectionResponseException(
            FailureType failureType,
            String message,
            RawApiResponse rawApiResponse,
            Throwable cause
    ) {
        super(message, cause);
        this.failureType = failureType;
        this.rawApiResponse = rawApiResponse;
    }

    public FailureType failureType() {
        return this.failureType;
    }

    public RawApiResponse rawApiResponse() {
        return this.rawApiResponse;
    }
}
