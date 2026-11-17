package com.todayfish.backend.collection.publicapi.model;

import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

// 실제 요청 조건과 받은 응답을 담는다.
public record RawApiResponse(
        URI endPoint, // 실제 호출한 API 주소
        Map<String, Object> requestParameters, // 실제 호출에 사용한 요청 조건
        int statusCode, // 받은 HTTP 상태 코드
        String contentType, // 받은 응답의 콘텐츠 형식
        byte[] body // 원본 응답 바이트
) {
    // 필수 값을 확인하고 요청 조건과 본문을 복사한다.
    public RawApiResponse {
        Objects.requireNonNull(endPoint, "endPoint");
        requestParameters = Map.copyOf(Objects.requireNonNull(requestParameters, "requestParameters"));

        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(body, "body");
        // 원본 바이트가 외부에서 바뀌지 않도록 복사한다.
        body = body.clone();

        if (HttpStatus.Series.resolve(statusCode) == null) {
            throw new IllegalArgumentException("Invalid HTTP status code" + statusCode);
        }
    }

    // 호출자가 원본 바이트를 바꾸지 못하도록 복사해 반환한다.
    @Override
    public byte[] body() {
        return body.clone();
    }
}
