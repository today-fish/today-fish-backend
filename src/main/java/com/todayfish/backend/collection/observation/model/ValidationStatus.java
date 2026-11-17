package com.todayfish.backend.collection.observation.model;

// 관측 후보의 검증 결과를 세 단계로 구분한다.
public enum ValidationStatus {
    VALID,
    REVIEW_REQUIRED,
    INVALID
}
