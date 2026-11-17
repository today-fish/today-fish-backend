package com.todayfish.backend.collection.publicapi.model;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// 기존 Request의 수집 조건을 Collector에 전달한다.
public record CollectionRequestCommand(
        UUID requestId, // 재시도에도 유지되는 Request 식별자
        UUID jobId, // 이 Request가 속한 Job 식별자
        Dataset dataset, // 수집할 데이터셋
        Long speciesId, // 수집 대상의 내부 어종 식별자
        LocalDate startDate, // 조회 기간의 시작일
        LocalDate endDate, // 조회 기간의 종료일
        Map<String, Object> parameters // 원천별 API 요청 조건
) {
    // 필수 값을 확인하고 요청 조건을 복사한다.
    public CollectionRequestCommand {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(dataset, "dataset");

        parameters = Map.copyOf(
                Objects.requireNonNull(parameters,"parameters")
        );

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "startDate must be before endDate"
            );
        }
    }
}
