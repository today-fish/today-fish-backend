package com.todayfish.backend.collection.publicapi.at;


import com.todayfish.backend.collection.publicapi.model.RawApiResponse;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class AtApiClient {
    private final RestClient restClient;
    private final AtApiProperties atApiProperties;
    private final static int MAX_RESPONSE_BYTE = 10 * 1024 * 1024; // 10MiB

    public AtApiClient(RestClient.Builder builder, AtApiProperties atApiProperties) {
        this.atApiProperties = atApiProperties;

        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(atApiProperties.connectTimeout())
                .withReadTimeout(atApiProperties.readTimeout());

        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        this.restClient = builder
                .requestFactory(factory)
                .baseUrl(atApiProperties.endPoint().toString())
                .build();

    }

    // Http 요청 fetch
    public RawApiResponse fetchTrades(
            LocalDate settlementDate,
            String marketCode,
            String largeCategoryCode,
            String middleCategoryCode,
            int pageNo) {
        Objects.requireNonNull(settlementDate, "settlementDate");
        if (marketCode == null || marketCode.isBlank()
                || largeCategoryCode == null || largeCategoryCode.isBlank()
                || middleCategoryCode == null || middleCategoryCode.isBlank()
                || pageNo < 1) {
            throw new IllegalArgumentException(
                    "Invalid aT request parameters"
            );
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("returnType", "JSON");
        parameters.put("pageNo", pageNo);
        parameters.put("numOfRows", atApiProperties.pageSize());
        parameters.put("cond[whsl_mrkt_cd::EQ]", marketCode);
        parameters.put("cond[trd_clcln_ymd::EQ]",
                settlementDate.toString());
        parameters.put("cond[gds_lclsf_cd::EQ]",
                largeCategoryCode);
        parameters.put("cond[gds_mclsf_cd::EQ]",
                middleCategoryCode);

        return restClient.get()
                .uri(urlBuilder -> {
                    urlBuilder.queryParam("serviceKey", "{serviceKey}");
                    parameters.forEach((key, value) -> urlBuilder.queryParam(key, value));
                    return urlBuilder.build(Map.of("serviceKey", atApiProperties.serviceKey()));
                })
                .exchange((request, response) -> {
                    byte[] body = response.getBody().readNBytes(MAX_RESPONSE_BYTE + 1);
                    if (body.length > MAX_RESPONSE_BYTE) {
                        throw new IllegalStateException("aT response exceeds maximum allowed size: " + MAX_RESPONSE_BYTE);
                    }
                    return new RawApiResponse(
                            atApiProperties.endPoint(),
                            parameters,
                            response.getStatusCode().value(),
                            Objects.toString(
                                    response.getHeaders().getFirst(
                                            HttpHeaders.CONTENT_TYPE
                                    ),
                                    ""
                            ),
                            body
                    );
                });
    }
}
