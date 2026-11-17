package com.todayfish.backend.collection.model;

import java.util.List;
import java.util.Objects;

public record MappingResult(
        List<NormalizedObservationCandidate> candidates
) {

    public MappingResult {
        candidates = List.copyOf(
                Objects.requireNonNull(candidates, "candidates")
        );
    }
}
