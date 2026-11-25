package com.todayfish.backend.collection.publicapi.kmi;

import com.todayfish.backend.collection.publicapi.kmi.dto.KmiPriceResponse;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

@Component
public class KmiResponseParser {

    private final JsonMapper jsonMapper;

    public KmiResponseParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public KmiPriceResponse parse(byte[] rawBody) {
        Objects.requireNonNull(rawBody, "rawBody");

        // 1. JSON 역직렬화
        KmiPriceResponse result;
        try {
            result = jsonMapper.readValue(rawBody, KmiPriceResponse.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to parse Kmi API response", e);
        }

        // 2. 응답 구조 검증
        if (result == null || result.data() == null) {
            throw new IllegalStateException("Missing KMI response data");
        }

        // 3. 배열 내부 구조 검증
        if (result.data().stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("KMI response contains null price item");
        }

        return result;
    }
}
