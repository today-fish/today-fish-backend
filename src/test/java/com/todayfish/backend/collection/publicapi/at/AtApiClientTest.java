
package com.todayfish.backend.collection.publicapi.at;

import com.sun.net.httpserver.HttpServer;
import com.todayfish.backend.collection.publicapi.model.RawApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AtApiClientTest {

    private static final LocalDate SETTLEMENT_DATE =
            LocalDate.of(2026, 11, 2);

    private static final int MAX_RESPONSE_BYTES = 10 * 1024 * 1024;

    private HttpServer server;
    private URI endpoint;
    private AtApiClient client;

    private final AtomicReference<String> receivedQuery =
            new AtomicReference<>();

    private volatile byte[] responseBody =
            "{}".getBytes(StandardCharsets.UTF_8);

    private volatile int responseStatus = 200;
    private volatile String responseContentType = "application/json";

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        server.createContext("/trades", exchange -> {
            receivedQuery.set(exchange.getRequestURI().getRawQuery());

            if (responseContentType != null) {
                exchange.getResponseHeaders().set(
                        "Content-Type",
                        responseContentType
                );
            }

            exchange.sendResponseHeaders(
                    responseStatus,
                    responseBody.length
            );

            try (var output = exchange.getResponseBody()) {
                output.write(responseBody);
            }
        });

        server.start();

        endpoint = URI.create(
                "http://127.0.0.1:"
                        + server.getAddress().getPort()
                        + "/trades"
        );

        client = createClient("test-key");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void 요청_조건을_aT_쿼리_파라미터로_변환한다() {
        RawApiResponse response = fetchTrades(client, 3);
        Map<String, String> query = parseQuery(receivedQuery.get());

        assertAll(
                () -> assertEquals("test-key", query.get("serviceKey")),
                () -> assertEquals("JSON", query.get("returnType")),
                () -> assertEquals("3", query.get("pageNo")),
                () -> assertEquals("2", query.get("numOfRows")),
                () -> assertEquals("240004",
                        query.get("cond[whsl_mrkt_cd::EQ]")),
                () -> assertEquals("2026-11-02",
                        query.get("cond[trd_clcln_ymd::EQ]")),
                () -> assertEquals("71",
                        query.get("cond[gds_lclsf_cd::EQ]")),
                () -> assertEquals("06",
                        query.get("cond[gds_mclsf_cd::EQ]"))
        );

        assertEquals(3, response.requestParameters().get("pageNo"));
        assertFalse(response.requestParameters().containsKey("serviceKey"));
    }

    @Test
    void 특수문자가_포함된_인증키를_정확히_전달한다() {
        String serviceKey = "test+key/&=value";
        AtApiClient specialKeyClient = createClient(serviceKey);

        fetchTrades(specialKeyClient, 1);

        Map<String, String> query = parseQuery(receivedQuery.get());

        assertEquals(serviceKey, query.get("serviceKey"));
        assertEquals("1", query.get("pageNo"));
    }

    @Test
    void HTTP_오류_응답도_원본으로_반환한다() {
        responseStatus = 503;
        responseContentType = "text/plain";
        responseBody = bytes("temporarily unavailable");

        RawApiResponse response = fetchTrades(client, 1);

        assertAll(
                () -> assertEquals(503, response.statusCode()),
                () -> assertEquals("text/plain", response.contentType()),
                () -> assertArrayEquals(responseBody, response.body())
        );
    }

    @Test
    void 응답_크기가_10MiB이면_허용한다() {
        responseBody = new byte[MAX_RESPONSE_BYTES];

        RawApiResponse response = fetchTrades(client, 1);

        assertEquals(MAX_RESPONSE_BYTES, response.body().length);
    }

    @Test
    void 응답_크기가_10MiB를_초과하면_거부한다() {
        responseBody = new byte[MAX_RESPONSE_BYTES + 1];

        assertThrows(
                IllegalStateException.class,
                () -> fetchTrades(client, 1)
        );
    }

    @Test
    void 잘못된_요청_조건은_HTTP_호출_전에_거부한다() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> client.fetchTrades(
                                null, "240004", "71", "06", 1
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> client.fetchTrades(
                                SETTLEMENT_DATE, " ", "71", "06", 1
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> client.fetchTrades(
                                SETTLEMENT_DATE, "240004", "71", "06", 0
                        )
                )
        );

        assertNull(receivedQuery.get());
    }

    private AtApiClient createClient(String serviceKey) {
        AtApiProperties properties = new AtApiProperties(
                endpoint,
                serviceKey,
                2,
                Duration.ofSeconds(2),
                Duration.ofSeconds(5)
        );

        return new AtApiClient(RestClient.builder(), properties);
    }

    private static RawApiResponse fetchTrades(
            AtApiClient apiClient,
            int pageNo
    ) {
        return apiClient.fetchTrades(
                SETTLEMENT_DATE,
                "240004",
                "71",
                "06",
                pageNo
        );
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        assertNotNull(rawQuery, "요청 쿼리가 없습니다.");

        Map<String, String> parameters = new HashMap<>();

        for (String entry : rawQuery.split("&")) {
            int separator = entry.indexOf('=');
            assertTrue(separator > 0, "잘못된 쿼리: " + entry);

            String key = URLDecoder.decode(
                    entry.substring(0, separator),
                    StandardCharsets.UTF_8
            );

            String value = URLDecoder.decode(
                    entry.substring(separator + 1),
                    StandardCharsets.UTF_8
            );

            assertNull(
                    parameters.put(key, value),
                    "중복 쿼리 파라미터: " + key
            );
        }

        return parameters;
    }
}
