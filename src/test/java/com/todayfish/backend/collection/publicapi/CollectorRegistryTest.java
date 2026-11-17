package com.todayfish.backend.collection.publicapi;

import com.todayfish.backend.collection.publicapi.model.CollectionRequestCommand;
import com.todayfish.backend.collection.publicapi.model.CollectionResult;
import com.todayfish.backend.collection.publicapi.model.Dataset;
import com.todayfish.backend.collection.publicapi.model.RawApiResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CollectorRegistryTest {

    @Test
    void 지원하는_Collector가_하나면_반환한다() {
        // Given
        CollectionResult expectedResult = new CollectionResult(
                new RawApiResponse(
                        URI.create("https://api.example.test/data"),
                        Map.of("page", 1),
                        200,
                        "application/json",
                        new byte[0]
                ),
                0,
                List.of()
        );
        PublicDataCollector atCollector = new MockCollector(Dataset.AT, expectedResult);
        PublicDataCollector kmiCollector = new MockCollector(Dataset.KMI, expectedResult);
        CollectorRegistry collectorRegistry = new CollectorRegistry(List.of(atCollector, kmiCollector));
        // when
        PublicDataCollector result = collectorRegistry.getCollector(Dataset.AT);

        // Then
        assertSame(atCollector, result);
    }

    @Test
    void 지원하는_Collector가_없으면_예외를_던진다() {
        // Given
        CollectionResult expectedResult = new CollectionResult(
                new RawApiResponse(
                        URI.create("https://api.example.test/data"),
                        Map.of("page", 1),
                        200,
                        "application/json",
                        new byte[0]
                ),
                0,
                List.of()
        );
        PublicDataCollector atCollector = new MockCollector(Dataset.AT, expectedResult);
        CollectorRegistry registry = new CollectorRegistry(List.of(atCollector));

        // When
        // Then
        assertThrows(IllegalArgumentException.class, () -> registry.getCollector(Dataset.KMI));
    }

    @Test
    void 지원하는_Collector가_둘이면_예외를_던진다() {
        // Given
        CollectionResult expectedResult = new CollectionResult(
                new RawApiResponse(
                        URI.create("https://api.example.test/data"),
                        Map.of("page", 1),
                        200,
                        "application/json",
                        new byte[0]
                ),
                0,
                List.of()
        );
        PublicDataCollector atCollector_1 = new MockCollector(Dataset.AT, expectedResult);
        PublicDataCollector atCollector_2 = new MockCollector(Dataset.AT, expectedResult);
        CollectorRegistry registry = new CollectorRegistry(List.of(atCollector_1, atCollector_2));
        // When
        // Then
        assertThrows(IllegalArgumentException.class,
                () -> registry.getCollector(Dataset.AT));
    }


    @Test
    void 선택한_Collector로_수집_요청을_실행할_수_있다() {
        // Given
        CollectionRequestCommand request =
                new CollectionRequestCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Dataset.AT,
                        1L,
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 17),
                        Map.of("page", 1)
                );

        CollectionResult expectedResult = new CollectionResult(
                new RawApiResponse(
                        URI.create("https://api.example.test/data"),
                        Map.of("page", 1),
                        200,
                        "application/json",
                        new byte[0]
                ),
                0,
                List.of()
        );

        MockCollector collector =
                new MockCollector(Dataset.AT, expectedResult);

        CollectorRegistry registry =
                new CollectorRegistry(List.of(collector));

        // When
        PublicDataCollector selected =
                registry.getCollector(Dataset.AT);

        CollectionResult actualResult = selected.collect(request);

        // Then
        assertSame(collector, selected);
        assertSame(request, collector.receivedRequest);
        assertSame(expectedResult, actualResult);
    }

    @Test
    void 데이터셋이_null이면_예외를_던진다() {
        CollectorRegistry registry =
                new CollectorRegistry(List.of());

        assertThrows(
                NullPointerException.class,
                () -> registry.getCollector(null)
        );
    }

    private static class MockCollector implements PublicDataCollector {

        private final Dataset suppotedDataset;
        private final CollectionResult expectedResult;

        private CollectionRequestCommand receivedRequest;

        MockCollector(Dataset suppotedDataset, CollectionResult expectedResult) {
            this.suppotedDataset = suppotedDataset;
            this.expectedResult = expectedResult;
        }

        @Override
        public boolean supports(Dataset dataset) {
            return suppotedDataset == dataset;
        }

        @Override
        public CollectionResult collect(CollectionRequestCommand request) {
            if (expectedResult == null) {
                throw new UnsupportedOperationException("Not required for registry tests");
            }

            receivedRequest = request;
            return expectedResult;
        }
    }
}

