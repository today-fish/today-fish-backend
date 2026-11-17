package com.todayfish.backend.collection.model;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

public record NormalizedObservationCandidate(
        Dataset dataset,
        Long speciesId,
        ObservationType observationType,
        LocalDate periodStart,
        LocalDate periodEnd,
        String sourceLocator,
        Map<String, Object> attributes
) {
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
