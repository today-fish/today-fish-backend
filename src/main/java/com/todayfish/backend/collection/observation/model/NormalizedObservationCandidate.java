package com.todayfish.backend.collection.observation.model;

import com.todayfish.backend.collection.publicapi.model.Dataset;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

// 원본에서 추출한 가격 또는 거래 관측 후보를 담는다.
public record NormalizedObservationCandidate(
        Dataset dataset, // 후보의 원천 데이터셋
        Long speciesId, // 연결된 내부 어종 식별자
        ObservationType observationType, // 가격 또는 거래 관측 유형
        LocalDate periodStart, // 관측 기간의 시작일
        LocalDate periodEnd, // 관측 기간의 종료일
        String sourceLocator, // 원본에서 값을 찾을 수 있는 위치
        Map<String, Object> attributes // 원천별 관측 상세 값
) {
    // 필수 값을 확인하고 상세 값을 복사한다.
    public NormalizedObservationCandidate {
        Objects.requireNonNull(dataset, "dataset");
        Objects.requireNonNull(
                observationType,
                "observationType"
        );

        attributes = Map.copyOf(
                Objects.requireNonNull(attributes, "attributes")
        );
    }
}
