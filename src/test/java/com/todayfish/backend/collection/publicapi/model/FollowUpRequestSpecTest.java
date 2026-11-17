
package com.todayfish.backend.collection.publicapi.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FollowUpRequestSpecTest {

    @Test
    void 종료일이_시작일보다_빠르면_예외를_던진다() {
        LocalDate startDate = LocalDate.of(2001, 1, 1);
        LocalDate endDate = LocalDate.of(2000, 1, 1);

        assertThrows(IllegalArgumentException.class,
                () -> new FollowUpRequestSpec(
                        startDate,
                        endDate,
                        Map.of("page", 2)
                )
        );
    }

    @Test
    void 시작일과_종료일이_같으면_허용한다() {
        LocalDate date = LocalDate.of(2000, 1, 1);

        assertDoesNotThrow(() -> new FollowUpRequestSpec(
                date,
                date,
                Map.of("page", 2)
        ));
    }

    @Test
    void 후속_요청_파라미터를_복사한다() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("page", 2);

        FollowUpRequestSpec spec = new FollowUpRequestSpec(
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 31),
                parameters
        );

        // When
        parameters.put("page", 3);

        // Then
        assertEquals(2, spec.parameters().get("page"));

        assertThrows(UnsupportedOperationException.class,
                () -> spec.parameters().put("page", 4)
        );
    }
}
