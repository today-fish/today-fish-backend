package com.todayfish.backend.collection.publicapi.at;

import com.todayfish.backend.collection.publicapi.PublicDataCollector;
import com.todayfish.backend.collection.publicapi.at.dto.AtTradesResponse;
import com.todayfish.backend.collection.publicapi.exception.AtProviderException;
import com.todayfish.backend.collection.publicapi.exception.CollectionResponseException;
import com.todayfish.backend.collection.publicapi.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AtCollector implements PublicDataCollector {

    private final AtApiClient apiClient;
    private final AtResponseParser responseParser;

    public AtCollector(AtApiClient apiClient, AtResponseParser responseParser) {
        this.apiClient = apiClient;
        this.responseParser = responseParser;
    }

    @Override
    public boolean supports(Dataset dataset) {
        return dataset == Dataset.AT;
    }

    @Override
    public CollectionResult collect(CollectionRequestCommand request) {
        Objects.requireNonNull(request, "request");

        if (!supports(request.dataset())) {
            throw new IllegalArgumentException("Unsupported dataset: " + request.dataset());
        }

        // aT API는 정산일자 단위로 조회한다.
        if (request.startDate() == null
                || !request.startDate().equals(request.endDate())) {
            throw new IllegalArgumentException("aT request requires a single settlement date");
        }

        Map<String, Object> parameters = request.parameters();
        String marketCode = requiredString(parameters, "marketCode");
        String largeCategoryCode = requiredString(parameters, "largeCategoryCode");
        String middleCategoryCode =
                requiredString(parameters, "middleCategoryCode");
        int pageNo = requiredPageNo(parameters);


        // 1. API 호출
        RawApiResponse rawResponse = apiClient.fetchTrades(
                request.startDate(),
                marketCode,
                largeCategoryCode,
                middleCategoryCode,
                pageNo
        );

        // 2. HTTP 상태 확인
        int statusCode = rawResponse.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new CollectionResponseException(
                    CollectionResponseException.FailureType.HTTP_ERROR,
                    "aT HTTP request failed: " + statusCode,
                    rawResponse,
                    null
            );
        }


        AtTradesResponse parsedResponse;
        // JSON 파싱 및 aT 응답 구조 검증
        try {
            parsedResponse = responseParser.parse(rawResponse.body());

            validatePagination(parsedResponse, rawResponse, pageNo);
        } catch (AtProviderException e) {
            throw new CollectionResponseException(
                    CollectionResponseException.FailureType.PROVIDER_ERROR,
                    "aT provider API error: " + e.resultCode(),
                    rawResponse,
                    e
            );
        } catch (IllegalStateException e) {
            throw new CollectionResponseException(
                    CollectionResponseException.FailureType.INVALID_RESPONSE,
                    "INVALID aT API response",
                    rawResponse,
                    e
            );
        }
        AtTradesResponse.Body body = parsedResponse.response().body();

        // 전체 페이지 데이터 건수
        int itemCount = countItems(body);

        // 5. 후속 페이지 생성
        List<FollowUpRequestSpec> followUpRequests = List.of();

        boolean hasNextPage = (long) body.pageNo() * body.numOfRows() < body.totalCount();
        if (hasNextPage) {
            Map<String, Object> nextParameters = new HashMap<>(parameters);
            nextParameters.put("pageNo", pageNo + 1);

            followUpRequests = List.of(
                    new FollowUpRequestSpec(
                            request.startDate(),
                            request.endDate(),
                            nextParameters
                    )
            );
        }

        return new CollectionResult(
                rawResponse,
                itemCount,
                followUpRequests
        );
    }

    private static String requiredString(
            Map<String, Object> parameters, String key
    ) {
        Object value = parameters.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Invalid aT parameter: " + key);
        }
        return text;
    }

    private static int requiredPageNo(Map<String, Object> parameters) {
        Object value = parameters.get("pageNo");

        if (!(value instanceof Integer pageNo) || pageNo < 1) {
            throw new IllegalArgumentException("Invalid at parameters: pageNo");
        }
        return pageNo;
    }

    private void validatePagination(AtTradesResponse parsed, RawApiResponse raw, int requestedPageNo) {
        AtTradesResponse.Body body = parsed.response().body();

        int pageNo = body.pageNo();
        int pageSize = body.numOfRows();
        int totalCount = body.totalCount();

        int itemCount = countItems(parsed.response().body());

        Object requestedSize = raw.requestParameters().get("numOfRows");

        if (!(requestedSize instanceof Number number)
                || pageSize != number.intValue()) {
            throw new IllegalStateException(
                    "aT response page size mismatch"
            );
        }

        if (pageNo != requestedPageNo) {
            throw new IllegalStateException(
                    "aT response page number mismatch"
            );
        }

        long offset = (long) (pageNo - 1) * pageSize;
        long remaining = Math.max(0L, (long) totalCount - offset);

        int expectedCount = (int) Math.min(pageSize, remaining);

        if (itemCount != expectedCount) {
            throw new IllegalStateException(
                    "aT response item count mismatch"
            );
        }
    }

    private static int countItems(AtTradesResponse.Body body) {
        return body.items() == null || body.items().item() == null ? 0 : body.items().item().size();
    }
}
