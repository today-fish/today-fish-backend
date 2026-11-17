package com.todayfish.backend.collection.publicapi;

import com.todayfish.backend.collection.publicapi.model.CollectionRequestCommand;
import com.todayfish.backend.collection.publicapi.model.CollectionResult;
import com.todayfish.backend.collection.publicapi.model.Dataset;

// 원천별 공공 API 수집에 공통으로 사용하는 계약이다.
public interface PublicDataCollector {
    // 이 Collector가 처리할 데이터셋인지 확인한다.
    boolean supports(Dataset dataset);
    // API 호출에 실패하면 예외를 던지고 성공하면 결과를 반환한다.
    CollectionResult collect(CollectionRequestCommand request);
}
