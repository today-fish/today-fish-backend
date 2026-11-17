package com.todayfish.backend.collection.publicapi.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CollectionRequestCommandTest {

    @Test
    void 종료일이_시작일보다_빠르면_예외를_던진다() {
        LocalDate startDate = LocalDate.of(2000, 1, 1); // 2000.01.01
        LocalDate endDate = LocalDate.of(2001, 1, 1); // 2001.01.01

        assertThrows(IllegalArgumentException.class,
                () -> new CollectionRequestCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Dataset.AT,
                        1L,
                        endDate, // startDate
                        startDate, // endDate
                        Map.of("key1", "val1")
                )
        );
    }

    @Test
    void 시작일과_종료일이_같으면_허용한다() {
        LocalDate date = LocalDate.of(2000, 1, 1); // 2000.01.01

        assertDoesNotThrow(() -> new CollectionRequestCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Dataset.AT,
                        1L,
                        date, // startDate
                        date, // endDate
                        Map.of("key1", "val1")
                )
        );
    }

    @Test
    void 요청_파라미터를_복사한다() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("page", 1);

        CollectionRequestCommand command =
                new CollectionRequestCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Dataset.AT,
                        1L,
                        LocalDate.of(2000, 1, 1),
                        LocalDate.of(2000, 1, 31),
                        parameters
                );

        // When
        parameters.put("page", 2);
        // Then
        assertEquals(1, command.parameters().get("page"));
        assertThrows(UnsupportedOperationException.class,
                () -> command.parameters().put("page", 3));
    }

    @Test
    void requestId가_null이면_예외를_던진다() {
        assertThrows(NullPointerException.class,
                () -> new CollectionRequestCommand(
                        null,
                        UUID.randomUUID(),
                        Dataset.AT,
                        1L,
                        null,
                        null,
                        Map.of()
                ));
    }
}
