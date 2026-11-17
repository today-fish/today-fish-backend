package com.todayfish.backend.collection.observation.model;

import java.util.List;
import java.util.Objects;

// 한 번의 매핑에서 얻은 관측 후보를 담는다.
public record MappingResult(
        List<NormalizedObservationCandidate> candidates // 원본 응답에서 추출한 후보 목록
) {

    // 후보 목록을 복사해 외부 변경을 막는다.
    public MappingResult {
        candidates = List.copyOf(
                Objects.requireNonNull(candidates, "candidates")
        );
    }
}
