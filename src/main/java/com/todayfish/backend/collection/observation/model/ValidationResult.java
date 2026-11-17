package com.todayfish.backend.collection.model;

import java.util.List;
import java.util.Objects;

public record ValidationResult(
        ValidationStatus status,
        List<String> reasons
) {
    public ValidationResult {
        Objects.requireNonNull(status, "statis");
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
    }
}
