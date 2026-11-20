package com.todayfish.backend.collection.publicapi.at;

import com.todayfish.backend.collection.publicapi.at.dto.AtTradesResponse;
import com.todayfish.backend.collection.publicapi.exception.AtProviderException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;

@Component
public class AtResponseParser {

    private final JsonMapper jsonMapper;

    public AtResponseParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    // Parsing간 응답 형식, 구조 검증은 파서가 담당한다.
    public AtTradesResponse parse(byte[] rawBody) {
        Objects.requireNonNull(rawBody, "rawBody");

        // 1. JSON 역직렬화
        AtTradesResponse result;
        try {
            result = jsonMapper.readValue(rawBody, AtTradesResponse.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to parse aT API response", e);
        }

        // 2. 응답 기본 구조 검증
        if (result == null || result.response() == null || result.response().header() == null) {
            throw new IllegalStateException("Missing aT response Header");
        }

        // 3. API 자체 결과 코드 확인
        AtTradesResponse.Header header = result.response().header();;
        if (!"0".equals(header.resultCode())) {
            throw new AtProviderException(header.resultCode());
        }

        // 4. 페이지 메타데이터 검증
        AtTradesResponse.Body body = result.response().body();
        if (body == null || body.numOfRows() == null || body.totalCount() == null || body.pageNo() == null) {
            throw new IllegalStateException("Missing aT pagination metadata");
        }

        if (body.pageNo() < 1 || body.numOfRows() <= 0 || body.totalCount() < 0) {
            throw new IllegalStateException("Invalid aT pagination metadata");
        }

        // 5. 데이터 배열 검증
        List<AtTradesResponse.TradeItem> items = body.items() == null ? null : body.items().item();

        if (body.totalCount() > 0 && items == null) {
            throw new IllegalStateException("Missing aT trade items");
        }

        if (items != null && items.size() > body.numOfRows()) {
            throw new IllegalStateException("aT item count exceeds page size");
        }

        return result;
    }
}
