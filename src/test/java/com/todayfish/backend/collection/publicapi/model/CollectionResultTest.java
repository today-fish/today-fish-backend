package com.todayfish.backend.collection.publicapi.model;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectionResultTest {

    @Test
    void 현재_페이지가_0건이면_데이터_없음으로_판단한다() {
        // Given
        CollectionResult result = new CollectionResult(rawResponse(), 0, List.of());
        // When
        // Then
        assertTrue(result.isNoData());
    }

    @Test
    void 후속_요청이_있어도_현재_페이지가_0건이면_데이터_없음이다() {
        // Given
        FollowUpRequestSpec nextPage = new FollowUpRequestSpec(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 17),
                Map.of("page", 2)
        );

        CollectionResult result = new CollectionResult(rawResponse(), 0, List.of(nextPage));
        // When
        // Then
        assertTrue(result.isNoData());
        assertEquals(1, result.followUpRequests().size());
        assertSame(nextPage, result.followUpRequests().getFirst());
    }

    @Test
    void 건수가_음수이면_예외를_던진다() {
        assertThrows(IllegalArgumentException.class,
                () -> new CollectionResult(rawResponse(), -1, List.of()));
    }

    @Test
    void 후속_요청_목록을_복사한다() {
        // Given
        List<FollowUpRequestSpec> requests = new ArrayList<>();

        FollowUpRequestSpec nextPage = new FollowUpRequestSpec(
                null,
                null,
                Map.of("page", 2)
        );

        requests.add(nextPage);

        CollectionResult result = new CollectionResult(
                rawResponse(),
                10,
                requests
        );
        // When
        requests.clear();

        // Then
        assertEquals(1, result.followUpRequests().size());
        assertThrows(UnsupportedOperationException.class, () -> result.followUpRequests().clear());
    }

    private RawApiResponse rawResponse() {
        return new RawApiResponse(URI.create("https://api/example.test/data"), Map.of("page", 1), 200, "application/json", "{}".getBytes(StandardCharsets.UTF_8));
    }
}