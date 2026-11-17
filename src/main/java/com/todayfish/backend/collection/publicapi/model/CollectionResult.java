package com.todayfish.backend.collection.publicapi.model;

import java.util.List;
import java.util.Objects;

// 현재 Request의 응답과 다음 요청 조건을 묶는다.
public record CollectionResult(
        RawApiResponse response, // 이번 API 호출의 원본 응답
        int itemCount, // 이번 응답 페이지의 데이터 건수
        List<FollowUpRequestSpec> followUpRequests // 이번 응답에서 발견한 다음 요청들
) {
    // 건수와 필수 값을 확인하고 후속 요청 목록을 복사한다.
    public CollectionResult {
        Objects.requireNonNull(response, "response");

        if (itemCount < 0) {
            throw new IllegalArgumentException("ItemCount must be positive");
        }

        followUpRequests = List.copyOf(
                Objects.requireNonNull(followUpRequests, "followUpRequests")
        );
    }

    // 이번 응답 페이지의 데이터가 0건인지 확인한다.
    public boolean isNoData() {
        return itemCount == 0;
    }
}
