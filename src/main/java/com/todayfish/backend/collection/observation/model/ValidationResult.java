package com.todayfish.backend.collection.observation.model;

import java.util.List;
import java.util.Objects;

// 관측 후보의 검증 상태와 사유를 담는다.
public record ValidationResult(
        ValidationStatus status, // 검증 결과의 상태
        List<String> reasons // 검토나 무효 판정의 사유
) {
    // 필수 값을 확인하고 사유 목록을 복사한다.
    public ValidationResult {
        Objects.requireNonNull(status, "statis");
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
    }
}
