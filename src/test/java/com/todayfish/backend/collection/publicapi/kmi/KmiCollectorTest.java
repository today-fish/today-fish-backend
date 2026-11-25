package com.todayfish.backend.collection.publicapi.kmi;

import com.todayfish.backend.collection.publicapi.exception.CollectionResponseException;
import com.todayfish.backend.collection.publicapi.kmi.dto.KmiPriceResponse;
import com.todayfish.backend.collection.publicapi.model.CollectionRequestCommand;
import com.todayfish.backend.collection.publicapi.model.CollectionResult;
import com.todayfish.backend.collection.publicapi.model.Dataset;
import com.todayfish.backend.collection.publicapi.model.RawApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KmiCollectorTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private static final LocalDate END = LocalDate.of(2026, 11, 20);

    private KmiApiClient apiClient;
    private KmiResponseParser parser;
    private KmiCollector collector;

    @BeforeEach
    void 준비한다() {
        apiClient = mock(KmiApiClient.class);
        parser = mock(KmiResponseParser.class);
        collector = new KmiCollector(apiClient, parser);
    }

    @Test
    void 기간_조회와_요청한_작업을_클라이언트에_전달한다() {
        RawApiResponse raw = raw(200);
        when(apiClient.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, START, END))
                .thenReturn(raw);
        when(parser.parse(any(byte[].class))).thenReturn(new KmiPriceResponse(List.of(
                item("2026-11-01", 0), item("2026-11-02", 25179), item("2026-11-02", 17931))));

        CollectionResult result = collector.collect(request(START, END, "PRODUCTION_PRICE"));

        assertSame(raw, result.response());
        assertEquals(3, result.itemCount());
        assertTrue(result.followUpRequests().isEmpty());
        verify(apiClient).fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, START, END);
    }

    @Test
    void 같은_시작일과_종료일로_수집할_수_있다() {
        RawApiResponse raw = raw(200);
        when(apiClient.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, START, START))
                .thenReturn(raw);
        when(parser.parse(any(byte[].class))).thenReturn(new KmiPriceResponse(List.of()));

        CollectionResult result = collector.collect(request(START, START, "PRODUCTION_PRICE"));

        assertSame(raw, result.response());
        verify(apiClient).fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, START, START);
    }

    @Test
    void 빈_응답은_원본을_보존한_0건_결과로_반환한다() {
        RawApiResponse raw = raw(200);
        when(apiClient.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, START, END))
                .thenReturn(raw);
        when(parser.parse(any(byte[].class))).thenReturn(new KmiPriceResponse(List.of()));

        CollectionResult result = collector.collect(request(START, END, "PRODUCTION_PRICE"));

        assertAll(
                () -> assertEquals(0, result.itemCount()),
                () -> assertTrue(result.isNoData()),
                () -> assertSame(raw, result.response()),
                () -> assertTrue(result.followUpRequests().isEmpty())
        );
    }

    @Test
    void 다른_데이터셋은_API_호출_전에_거부한다() {
        CollectionRequestCommand request = new CollectionRequestCommand(
                UUID.randomUUID(), UUID.randomUUID(), Dataset.AT, 1L, START, END,
                Map.of("speciesCode", "KDFSH08", "operation", "PRODUCTION_PRICE"));

        assertThrows(IllegalArgumentException.class, () -> collector.collect(request));
        verifyNoInteractions(apiClient, parser);
    }

    @Test
    void 도매와_소매_작업을_요청_파라미터로_선택한다() {
        RawApiResponse raw = raw(200);
        when(apiClient.fetchPrices(eq("KDFSH08"), any(KmiApiOperation.class), eq(START), eq(END)))
                .thenReturn(raw);
        when(parser.parse(any(byte[].class))).thenReturn(new KmiPriceResponse(List.of()));

        collector.collect(request(START, END, "WHOLESALE_PRICE"));
        collector.collect(request(START, END, "RETAIL_PRICE"));

        verify(apiClient).fetchPrices("KDFSH08", KmiApiOperation.WHOLESALE_PRICE, START, END);
        verify(apiClient).fetchPrices("KDFSH08", KmiApiOperation.RETAIL_PRICE, START, END);
    }

    @Test
    void 날짜나_요청_파라미터가_잘못되면_API를_호출하지_않는다() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> collector.collect(request(null, END, "PRODUCTION_PRICE"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> collector.collect(request(START, null, "PRODUCTION_PRICE"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> collector.collect(request(START, END, "INVALID"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> collector.collect(new CollectionRequestCommand(UUID.randomUUID(), UUID.randomUUID(),
                                Dataset.KMI, 1L, START, END, Map.of("operation", "PRODUCTION_PRICE"))))
        );
        verifyNoInteractions(apiClient, parser);
    }

    @Test
    void 잘못된_날짜_순서는_요청_계약에서_거부한다() {
        assertThrows(IllegalArgumentException.class,
                () -> request(END, START, "PRODUCTION_PRICE"));
    }

    @Test
    void HTTP_오류는_원본을_포함해_전달한다() {
        RawApiResponse raw = raw(503);
        when(apiClient.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, START, END))
                .thenReturn(raw);

        CollectionResponseException error = assertThrows(CollectionResponseException.class,
                () -> collector.collect(request(START, END, "PRODUCTION_PRICE")));

        assertEquals(CollectionResponseException.FailureType.HTTP_ERROR, error.failureType());
        assertSame(raw, error.rawApiResponse());
        verifyNoInteractions(parser);
    }

    @Test
    void 파싱_오류는_원본과_원인을_포함해_전달한다() {
        RawApiResponse raw = raw(200);
        IllegalStateException cause = new IllegalStateException("invalid KMI response");
        when(apiClient.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, START, END))
                .thenReturn(raw);
        when(parser.parse(any(byte[].class))).thenThrow(cause);

        CollectionResponseException error = assertThrows(CollectionResponseException.class,
                () -> collector.collect(request(START, END, "PRODUCTION_PRICE")));

        assertEquals(CollectionResponseException.FailureType.INVALID_RESPONSE, error.failureType());
        assertSame(raw, error.rawApiResponse());
        assertSame(cause, error.getCause());
    }

    @Test
    void 실제_파싱_실패도_원본과_원인_예외를_보존한다() {
        RawApiResponse raw = new RawApiResponse(URI.create("https://api.example.test/kmi"),
                Map.of("kdfshCode", "KDFSH08"), 200, "application/json",
                "{".getBytes(StandardCharsets.UTF_8));
        when(apiClient.fetchPrices("KDFSH08", KmiApiOperation.PRODUCTION_PRICE, START, END))
                .thenReturn(raw);
        KmiCollector collectorWithRealParser = new KmiCollector(apiClient,
                new KmiResponseParser(JsonMapper.builder().build()));

        CollectionResponseException error = assertThrows(CollectionResponseException.class,
                () -> collectorWithRealParser.collect(request(START, END, "PRODUCTION_PRICE")));

        assertEquals(CollectionResponseException.FailureType.INVALID_RESPONSE, error.failureType());
        assertSame(raw, error.rawApiResponse());
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertNotNull(error.getCause().getCause());
    }

    private static CollectionRequestCommand request(LocalDate start, LocalDate end, String operation) {
        return new CollectionRequestCommand(UUID.randomUUID(), UUID.randomUUID(), Dataset.KMI, 1L,
                start, end, Map.of("speciesCode", "KDFSH08", "operation", operation));
    }

    private static RawApiResponse raw(int status) {
        return new RawApiResponse(URI.create("https://api.example.test/kmi"),
                Map.of("kdfshCode", "KDFSH08", "subAction", "prdlcPrice"), status,
                "application/json", "{\"data\":[]}".getBytes(StandardCharsets.UTF_8));
    }

    private static KmiPriceResponse.PriceItem item(String date, int price) {
        return new KmiPriceResponse.PriceItem(date, "KDFSH08", "산지", "SLE03",
                java.math.BigDecimal.valueOf(price));
    }
}
