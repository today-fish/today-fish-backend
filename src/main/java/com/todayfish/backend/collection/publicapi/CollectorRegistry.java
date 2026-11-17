package com.todayfish.backend.collection.publicapi;

import com.todayfish.backend.collection.publicapi.model.Dataset;

import java.util.List;
import java.util.Objects;

// 데이터셋에 맞는 Collector를 선택한다.
public class CollectorRegistry {
    // 등록된 Collector 목록을 보관한다.
    private final List<PublicDataCollector> collectors;

    // 등록 목록을 복사해 외부 변경을 막는다.
    public CollectorRegistry(List<PublicDataCollector> collectors) {
        this.collectors = List.copyOf(Objects.requireNonNull(collectors, "collectors"));
    }

    // 지원하는 Collector가 정확히 하나일 때 반환한다.
    public PublicDataCollector getCollector(Dataset dataset) {
        Objects.requireNonNull(dataset, "dataset");

        List<PublicDataCollector> matches = collectors.stream()
                .filter(collector -> collector.supports(dataset))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Unsupported datasets: " + dataset);
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException("Multiple collectors for datasets: " + dataset);
        }
        return matches.getFirst();
    }
}
