package com.todayfish.backend.collection.observation;

import com.todayfish.backend.collection.observation.model.NormalizedObservationCandidate;
import com.todayfish.backend.collection.observation.model.ValidationResult;

// 관측 후보의 유효성을 판단하는 계약이다.
public interface PublicDataValidator {
    // 후보를 검사하고 상태와 사유를 반환한다.
    ValidationResult validate(NormalizedObservationCandidate candidate);
}
