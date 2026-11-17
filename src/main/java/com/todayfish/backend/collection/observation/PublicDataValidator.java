package com.todayfish.backend.collection.observation;

import com.todayfish.backend.collection.model.NormalizedObservationCandidate;
import com.todayfish.backend.collection.model.ValidationResult;

public interface PublicValidator {
    ValidationResult validate(NormalizedObservationCandidate candidate);
}
