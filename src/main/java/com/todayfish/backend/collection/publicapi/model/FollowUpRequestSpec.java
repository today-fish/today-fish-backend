package com.todayfish.backend.collection.publicapi.model;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

// 현재 요청에서 발견한 다음 요청의 조건을 담는다.
public record FollowUpRequestSpec(
        LocalDate startDate, // 다음 요청의 조회 시작일
        LocalDate endDate, // 다음 요청의 조회 종료일
        Map<String, Object> parameters // 다음 페이지 등에 사용할 요청 조건
) {
    // 요청 조건을 복사하고 날짜 범위를 확인한다.
    public FollowUpRequestSpec {
        parameters = Map.copyOf(Objects.requireNonNull(parameters, "parameters"));

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid data range");
        }
    }
}
