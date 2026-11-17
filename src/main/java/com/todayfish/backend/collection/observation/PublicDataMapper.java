package com.todayfish.backend.collection.observation;

import com.todayfish.backend.collection.publicapi.model.Dataset;
import com.todayfish.backend.collection.observation.model.MappingResult;
import com.todayfish.backend.collection.publicapi.model.RawApiResponse;

// API 응답을 공통 관측 후보로 바꾸는 계약이다.
public interface PublicDataMapper {
    // 이 Mapper가 처리할 데이터셋인지 확인한다.
    boolean supports(Dataset dataset);
    // 원본 응답에서 관측 후보를 추출한다.
    MappingResult map(RawApiResponse response, Dataset dataset);
}
