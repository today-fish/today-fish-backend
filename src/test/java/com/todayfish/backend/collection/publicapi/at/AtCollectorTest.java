
package com.todayfish.backend.collection.publicapi.at;

import com.todayfish.backend.collection.publicapi.at.dto.AtTradesResponse;
import com.todayfish.backend.collection.publicapi.exception.AtProviderException;
import com.todayfish.backend.collection.publicapi.exception.CollectionResponseException;
import com.todayfish.backend.collection.publicapi.model.CollectionRequestCommand;
import com.todayfish.backend.collection.publicapi.model.CollectionResult;
import com.todayfish.backend.collection.publicapi.model.Dataset;
import com.todayfish.backend.collection.publicapi.model.FollowUpRequestSpec;
import com.todayfish.backend.collection.publicapi.model.RawApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AtCollectorTest {

    private static final LocalDate SETTLEMENT_DATE =
            LocalDate.of(2026, 11, 2);

    private AtApiClient apiClient;
    private AtResponseParser responseParser;
    private AtCollector collector;

    @BeforeEach
    void setUp() {
        apiClient = mock(AtApiClient.class);
        responseParser = mock(AtResponseParser.class);
        collector = new AtCollector(apiClient, responseParser);
    }

    @Test
    void aT_데이터셋만_지원한다() {
        assertTrue(collector.supports(Dataset.AT));
        assertFalse(collector.supports(Dataset.KMI));
    }

    @Test
    void 정산일자_범위가_하루가_아니면_거부한다() {
        CollectionRequestCommand request = createRequest(
                Dataset.AT,
                SETTLEMENT_DATE,
                SETTLEMENT_DATE.plusDays(1),
                requestParameters(1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> collector.collect(request)
        );

        verifyNoInteractions(apiClient, responseParser);
    }

    @Test
    void 필수_요청_조건이_없거나_잘못되면_거부한다() {
        CollectionRequestCommand missingMarket = validRequest(
                Map.of("pageNo", 1)
        );

        CollectionRequestCommand invalidPage = validRequest(
                Map.of(
                        "marketCode", "240004",
                        "largeCategoryCode", "71",
                        "middleCategoryCode", "06",
                        "pageNo", 0
                )
        );

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> collector.collect(missingMarket)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> collector.collect(invalidPage)
                )
        );

        verifyNoInteractions(apiClient, responseParser);
    }

    @Test
    void 첫_페이지에서_후속_페이지_요청을_생성한다() {
        RawApiResponse raw = rawResponse(200, 1, 2);

        stubClient(1, raw);
        when(responseParser.parse(any(byte[].class)))
                .thenReturn(tradesResponse(1, 2, 3, 2));

        CollectionResult result =
                collector.collect(validRequest(requestParameters(1)));

        assertAll(
                () -> assertSame(raw, result.response()),
                () -> assertEquals(2, result.itemCount()),
                () -> assertFalse(result.isNoData()),
                () -> assertEquals(1, result.followUpRequests().size())
        );

        FollowUpRequestSpec next = result.followUpRequests().getFirst();

        assertAll(
                () -> assertEquals(SETTLEMENT_DATE, next.startDate()),
                () -> assertEquals(SETTLEMENT_DATE, next.endDate()),
                () -> assertEquals(2, next.parameters().get("pageNo")),
                () -> assertEquals("240004",
                        next.parameters().get("marketCode")),
                () -> assertEquals("71",
                        next.parameters().get("largeCategoryCode")),
                () -> assertEquals("06",
                        next.parameters().get("middleCategoryCode"))
        );
    }

    @Test
    void 마지막_페이지에서는_후속_요청을_생성하지_않는다() {
        RawApiResponse raw = rawResponse(200, 2, 2);

        stubClient(2, raw);
        when(responseParser.parse(any(byte[].class)))
                .thenReturn(tradesResponse(2, 2, 3, 1));

        CollectionResult result =
                collector.collect(validRequest(requestParameters(2)));

        assertEquals(1, result.itemCount());
        assertTrue(result.followUpRequests().isEmpty());
    }

    @Test
    void 정상_0건_응답은_빈_결과를_반환한다() {
        RawApiResponse raw = rawResponse(200, 1, 2);

        stubClient(1, raw);
        when(responseParser.parse(any(byte[].class)))
                .thenReturn(tradesResponse(1, 2, 0, 0));

        CollectionResult result =
                collector.collect(validRequest(requestParameters(1)));

        assertAll(
                () -> assertSame(raw, result.response()),
                () -> assertEquals(0, result.itemCount()),
                () -> assertTrue(result.isNoData()),
                () -> assertTrue(result.followUpRequests().isEmpty())
        );
    }

    @Test
    void HTTP_오류는_HTTP_ERROR와_원본_응답을_전달한다() {
        RawApiResponse raw = rawResponse(503, 1, 2);

        stubClient(1, raw);

        CollectionResponseException exception = assertThrows(
                CollectionResponseException.class,
                () -> collector.collect(
                        validRequest(requestParameters(1))
                )
        );

        assertEquals(
                CollectionResponseException.FailureType.HTTP_ERROR,
                exception.failureType()
        );
        assertSame(raw, exception.rawApiResponse());

        verifyNoInteractions(responseParser);
    }

    @Test
    void 제공기관_오류는_PROVIDER_ERROR와_결과코드를_전달한다() {
        RawApiResponse raw = rawResponse(200, 1, 2);

        stubClient(1, raw);
        when(responseParser.parse(any(byte[].class)))
                .thenThrow(new AtProviderException("99"));

        CollectionResponseException exception = assertThrows(
                CollectionResponseException.class,
                () -> collector.collect(
                        validRequest(requestParameters(1))
                )
        );

        assertEquals(
                CollectionResponseException.FailureType.PROVIDER_ERROR,
                exception.failureType()
        );
        assertSame(raw, exception.rawApiResponse());

        AtProviderException cause = assertInstanceOf(
                AtProviderException.class,
                exception.getCause()
        );

        assertEquals("99", cause.resultCode());
    }

    @Test
    void 응답_구조_오류는_INVALID_RESPONSE와_원인을_전달한다() {
        RawApiResponse raw = rawResponse(200, 1, 2);
        IllegalStateException cause =
                new IllegalStateException("invalid JSON");

        stubClient(1, raw);
        when(responseParser.parse(any(byte[].class)))
                .thenThrow(cause);

        CollectionResponseException exception = assertThrows(
                CollectionResponseException.class,
                () -> collector.collect(
                        validRequest(requestParameters(1))
                )
        );

        assertEquals(
                CollectionResponseException.FailureType.INVALID_RESPONSE,
                exception.failureType()
        );
        assertSame(raw, exception.rawApiResponse());
        assertSame(cause, exception.getCause());
    }

    @Test
    void 응답_페이지_번호가_요청과_다르면_거부한다() {
        RawApiResponse raw = rawResponse(200, 1, 2);

        stubClient(1, raw);
        when(responseParser.parse(any(byte[].class)))
                .thenReturn(tradesResponse(2, 2, 3, 1));

        assertInvalidResponse(raw, 1);
    }

    @Test
    void 응답_페이지_크기가_요청과_다르면_거부한다() {
        RawApiResponse raw = rawResponse(200, 1, 2);

        stubClient(1, raw);
        when(responseParser.parse(any(byte[].class)))
                .thenReturn(tradesResponse(1, 3, 3, 3));

        assertInvalidResponse(raw, 1);
    }

    @Test
    void 응답_항목_수가_예상_건수와_다르면_거부한다() {
        RawApiResponse raw = rawResponse(200, 1, 2);

        stubClient(1, raw);
        when(responseParser.parse(any(byte[].class)))
                .thenReturn(tradesResponse(1, 2, 2, 1));

        assertInvalidResponse(raw, 1);
    }

    private void stubClient(int pageNo, RawApiResponse raw) {
        when(apiClient.fetchTrades(
                SETTLEMENT_DATE,
                "240004",
                "71",
                "06",
                pageNo
        )).thenReturn(raw);
    }

    private void assertInvalidResponse(
            RawApiResponse expectedRaw,
            int pageNo
    ) {
        CollectionResponseException exception = assertThrows(
                CollectionResponseException.class,
                () -> collector.collect(
                        validRequest(requestParameters(pageNo))
                )
        );

        assertEquals(
                CollectionResponseException.FailureType.INVALID_RESPONSE,
                exception.failureType()
        );
        assertSame(expectedRaw, exception.rawApiResponse());
        assertNotNull(exception.getCause());
    }

    private static CollectionRequestCommand validRequest(
            Map<String, Object> parameters
    ) {
        return createRequest(
                Dataset.AT,
                SETTLEMENT_DATE,
                SETTLEMENT_DATE,
                parameters
        );
    }

    private static CollectionRequestCommand createRequest(
            Dataset dataset,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, Object> parameters
    ) {
        return new CollectionRequestCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                dataset,
                1L,
                startDate,
                endDate,
                parameters
        );
    }

    private static Map<String, Object> requestParameters(int pageNo) {
        return Map.of(
                "marketCode", "240004",
                "largeCategoryCode", "71",
                "middleCategoryCode", "06",
                "pageNo", pageNo
        );
    }

    private static RawApiResponse rawResponse(
            int statusCode,
            int pageNo,
            int pageSize
    ) {
        return new RawApiResponse(
                URI.create("https://api.example.test/trades"),
                Map.of(
                        "pageNo", pageNo,
                        "numOfRows", pageSize
                ),
                statusCode,
                "application/json",
                "{}".getBytes(StandardCharsets.UTF_8)
        );
    }

    private static AtTradesResponse tradesResponse(
            int pageNo,
            int pageSize,
            int totalCount,
            int itemCount
    ) {
        AtTradesResponse.TradeItem item =
                new AtTradesResponse.TradeItem(
                        "2026-11-02",
                        "240004",
                        "71",
                        "06",
                        "01",
                        "kg",
                        "1",
                        "1",
                        "100",
                        "100"
                );

        List<AtTradesResponse.TradeItem> items =
                Collections.nCopies(itemCount, item);

        return new AtTradesResponse(
                new AtTradesResponse.Response(
                        new AtTradesResponse.Header("0", "정상"),
                        new AtTradesResponse.Body(
                                pageNo,
                                pageSize,
                                totalCount,
                                new AtTradesResponse.Item(items)
                        )
                )
        );
    }
}
