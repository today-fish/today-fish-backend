package com.todayfish.backend.collection.publicapi.kmi;


import com.todayfish.backend.collection.publicapi.model.RawApiResponse;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class KmiApiClient {
    private final RestClient restClient;
    private final KmiApiProperties kmiApiProperties;
    private static final int MAX_RESPONSE_BYTES = 10 * 1024 * 1024;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public KmiApiClient(RestClient.Builder builder, KmiApiProperties kmiApiProperties) {
        this.kmiApiProperties = kmiApiProperties;

        HttpClientSettings settings = HttpClientSettings.defaults()
                .withReadTimeout(kmiApiProperties.readTimeout())
                .withConnectTimeout(kmiApiProperties.connectTimeout());

        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        this.restClient = builder.requestFactory(factory)
                .baseUrl(kmiApiProperties.endPoint().toString())
                .build();
    }

    // Http 요청 fetch
    public RawApiResponse fetchPrices(String speciesCode,
                                     KmiApiOperation operation,
                                     LocalDate startDate,
                                     LocalDate endDate) {

        if (speciesCode == null || speciesCode.isBlank()) {
            throw new IllegalArgumentException("Invalid KMI species Code");
        }

        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("KMI startDate must not be after endDate");
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("kdfshCode", speciesCode);
        parameters.put("subAction", operation.subAction());
        parameters.put("startDt", startDate.format(DATE_FORMAT));
        parameters.put("endDt", endDate.format(DATE_FORMAT));

        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.queryParam("key", kmiApiProperties.serviceKey());
                    parameters.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .exchange((request, response) -> {
                    byte[] body = response.getBody()
                            .readNBytes(MAX_RESPONSE_BYTES + 1);

                    if (body.length > MAX_RESPONSE_BYTES) {
                        throw new IllegalStateException("KMI response exceeds maximum allowed size: " + MAX_RESPONSE_BYTES);
                    }

                    return new RawApiResponse(
                            kmiApiProperties.endPoint(),
                            parameters,
                            response.getStatusCode().value(),
                            Objects.toString(
                                    response.getHeaders()
                                            .getFirst(HttpHeaders.CONTENT_TYPE),
                                    ""
                            ),
                            body
                    );
                });
    }
}
