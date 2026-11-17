
package com.todayfish.backend.collection.publicapi.model;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RawApiResponseTest {

    @Test
    void 생성_후_원본_바이트를_바꿔도_응답은_유지된다() {
        // Given
        byte[] body = "original".getBytes(StandardCharsets.UTF_8);

        RawApiResponse response = rawResponse(
                body,
                Map.of("page", "1"),
                200
        );

        // When
        body[0] = 'X';

        // Then
        assertArrayEquals(
                "original".getBytes(StandardCharsets.UTF_8),
                response.body()
        );
    }

    @Test
    void 반환받은_바이트를_바꿔도_응답은_유지된다() {
        // Given
        RawApiResponse response = rawResponse(
                "original".getBytes(StandardCharsets.UTF_8),
                Map.of("page", "1"),
                200
        );

        // When
        byte[] returnedBody = response.body();
        returnedBody[0] = 'X';

        // Then
        assertArrayEquals(
                "original".getBytes(StandardCharsets.UTF_8),
                response.body()
        );
    }

    @Test
    void 실제_요청_파라미터를_복사한다() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("page", "1");

        RawApiResponse response = rawResponse(
                new byte[0],
                parameters,
                200
        );

        // When
        parameters.put("page", "2");

        // Then
        assertEquals(
                "1",
                response.requestParameters().get("page")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> response.requestParameters().put("page", "3")
        );
    }

    @Test
    void 유효하지_않은_HTTP_상태는_거부한다() {
        // Given & When & Then
        assertThrows(
                IllegalArgumentException.class,
                () -> rawResponse(new byte[0], Map.of(), 99)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> rawResponse(new byte[0], Map.of(), 600)
        );

        // 유효한 서버 오류 응답도 표현 가능해야 한다.
        assertDoesNotThrow(
                () -> rawResponse(new byte[0], Map.of(), 503)
        );

        assertDoesNotThrow(
                () -> rawResponse(new byte[0], Map.of(), 504)
        );

        assertDoesNotThrow(
                () -> rawResponse(new byte[0], Map.of(), 599)
        );
    }

    private RawApiResponse rawResponse(
            byte[] body,
            Map<String, Object> parameters,
            int statusCode
    ) {
        return new RawApiResponse(
                URI.create("https://api.example.test/data"),
                parameters,
                statusCode,
                "application/json",
                body
        );
    }
}
