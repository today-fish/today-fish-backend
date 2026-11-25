package com.todayfish.backend.collection.publicapi.kmi;

import com.sun.net.httpserver.HttpServer;
import com.todayfish.backend.collection.publicapi.model.RawApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class KmiApiClientTest {

    private HttpServer server;

    @AfterEach
    void 서버를_중지한다() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void 기간과_작업을_요청하고_인증키는_원본_조건에서_제외한다() throws IOException {
        AtomicReference<String> query = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/API/openAPI.do", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] body = "{\"data\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();

        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/API/openAPI.do");
        KmiApiProperties properties = new KmiApiProperties(endpoint, "test-key",
                Duration.ofSeconds(2), Duration.ofSeconds(5));
        KmiApiClient client = new KmiApiClient(RestClient.builder(), properties);

        RawApiResponse response = client.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20));

        Map<String, String> parameters = Arrays.stream(query.get().split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(part -> part[0], part -> part[1]));
        assertAll(
                () -> assertEquals("test-key", parameters.get("key")),
                () -> assertEquals("KDFSH08", parameters.get("kdfshCode")),
                () -> assertEquals("prdlcPrice", parameters.get("subAction")),
                () -> assertEquals("20261101", parameters.get("startDt")),
                () -> assertEquals("20261120", parameters.get("endDt")),
                () -> assertFalse(response.requestParameters().containsKey("key")),
                () -> assertEquals(200, response.statusCode())
        );
    }

    @Test
    void 응답이_10MiB를_넘으면_외부_응답_오류로_처리한다() throws IOException {
        byte[] body = new byte[10 * 1024 * 1024 + 1];
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/API/openAPI.do", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();

        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/API/openAPI.do");
        KmiApiClient client = new KmiApiClient(RestClient.builder(),
                new KmiApiProperties(endpoint, "test-key", Duration.ofSeconds(2), Duration.ofSeconds(5)));

        assertThrows(IllegalStateException.class,
                () -> client.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE,
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2)));
    }

    @Test
    void HTTP_오류_응답의_상태와_본문을_원본으로_반환한다() throws IOException {
        byte[] body = "KMI unavailable".getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/API/openAPI.do", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(503, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/API/openAPI.do");
        KmiApiClient client = new KmiApiClient(RestClient.builder(),
                new KmiApiProperties(endpoint, "test-key", Duration.ofSeconds(2), Duration.ofSeconds(5)));

        RawApiResponse response = client.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));

        assertAll(
                () -> assertEquals(503, response.statusCode()),
                () -> assertArrayEquals(body, response.body()),
                () -> assertEquals("text/plain", response.contentType())
        );
    }

    @Test
    void 응답_크기가_정확히_10MiB이면_허용한다() throws IOException {
        byte[] body = new byte[10 * 1024 * 1024];
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/API/openAPI.do", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/API/openAPI.do");
        KmiApiClient client = new KmiApiClient(RestClient.builder(),
                new KmiApiProperties(endpoint, "test-key", Duration.ofSeconds(2), Duration.ofSeconds(5)));

        RawApiResponse response = client.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2));

        assertEquals(body.length, response.body().length);
    }

    @Test
    void 잘못된_요청값은_HTTP_호출_전에_거부한다() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/API/openAPI.do", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/API/openAPI.do");
        KmiApiClient client = new KmiApiClient(RestClient.builder(),
                new KmiApiProperties(endpoint, "test-key", Duration.ofSeconds(2), Duration.ofSeconds(5)));
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 2);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> client.fetchPrices(" ", KmiApiOperation.PRODUCTION_PRICE, start, end)),
                () -> assertThrows(NullPointerException.class,
                        () -> client.fetchPrices("KDFSH08", null, start, end)),
                () -> assertThrows(NullPointerException.class,
                        () -> client.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, null, end)),
                () -> assertThrows(NullPointerException.class,
                        () -> client.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, start, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> client.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, end, start))
        );
        assertEquals(0, requestCount.get());
    }

    @Test
    void 도매와_소매_작업의_subAction을_전송한다() throws IOException {
        List<String> queries = new CopyOnWriteArrayList<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/API/openAPI.do", exchange -> {
            queries.add(exchange.getRequestURI().getRawQuery());
            byte[] body = "{\"data\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/API/openAPI.do");
        KmiApiClient client = new KmiApiClient(RestClient.builder(),
                new KmiApiProperties(endpoint, "test-key", Duration.ofSeconds(2), Duration.ofSeconds(5)));
        LocalDate date = LocalDate.of(2026, 11, 1);

        client.fetchPrices("KDFSH08", KmiApiOperation.WHOLESALE_PRICE, date, date);
        client.fetchPrices("KDFSH08", KmiApiOperation.RETAIL_PRICE, date, date);

        assertEquals(2, queries.size());
        assertTrue(queries.get(0).contains("subAction=whsalPrice"));
        assertTrue(queries.get(1).contains("subAction=rtlsalPrice"));
    }
}
