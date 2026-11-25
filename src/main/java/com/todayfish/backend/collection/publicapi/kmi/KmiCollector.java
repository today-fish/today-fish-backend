package com.todayfish.backend.collection.publicapi.kmi;

import com.todayfish.backend.collection.publicapi.PublicDataCollector;
import com.todayfish.backend.collection.publicapi.exception.CollectionResponseException;
import com.todayfish.backend.collection.publicapi.kmi.dto.KmiPriceResponse;
import com.todayfish.backend.collection.publicapi.model.CollectionRequestCommand;
import com.todayfish.backend.collection.publicapi.model.CollectionResult;
import com.todayfish.backend.collection.publicapi.model.Dataset;
import com.todayfish.backend.collection.publicapi.model.RawApiResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class KmiCollector implements PublicDataCollector {
    private final KmiApiClient apiClient;
    private final KmiResponseParser parser;

    public KmiCollector(KmiApiClient apiClient, KmiResponseParser parser) {
        this.apiClient = apiClient;
        this.parser = parser;
    }

    @Override
    public boolean supports(Dataset dataset) {
        return dataset == Dataset.KMI;
    }

    @Override
    public CollectionResult collect(CollectionRequestCommand request) {
        Objects.requireNonNull(request, "request");

        if (!supports(request.dataset())) {
            throw new IllegalArgumentException("Unsupported dataset: " + request.dataset());
        }

        if (request.startDate() == null || request.endDate() == null) {
            throw new IllegalArgumentException("KMI request requires startDate and endDate");
        }
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("KMI startDate must not be after endDate");
        }

        Map<String, Object> parameters = request.parameters();
        String speciesCode = requiredString(parameters, "speciesCode");
        KmiApiOperation operation = requiredOperation(parameters);

        // 1. KMI API 호출
        RawApiResponse rawResponse = apiClient.fetchPrices(
                speciesCode,
                operation,
                request.startDate(),
                request.endDate()
        );

        // 2. HTTP 상태 확인
        int statusCode = rawResponse.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new CollectionResponseException(
                    CollectionResponseException.FailureType.HTTP_ERROR,
                    "KMI HTTP request failed: " + statusCode,
                    rawResponse,
                    null
            );
        }

        KmiPriceResponse parsedResponse;
        // JSON 파싱 및 KMI 응답 구조 검증
        try {
            parsedResponse = parser.parse(rawResponse.body());

        } catch (IllegalStateException e) {
            throw new CollectionResponseException(
                    CollectionResponseException.FailureType.INVALID_RESPONSE,
                    "INVALID KMI API response",
                    rawResponse,
                    e
            );
        }
        List<KmiPriceResponse.PriceItem> data = parsedResponse.data();

        return new CollectionResult(rawResponse, data.size(), List.of());
    }

    private static String requiredString(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Invalid KMI parameter: " + key);
        }
        return text;
    }

    private static KmiApiOperation requiredOperation(Map<String, Object> parameters) {
        String value = requiredString(parameters, "operation");

        try {
            return KmiApiOperation.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid KMI parameters: operation", e);
        }

    }
}
