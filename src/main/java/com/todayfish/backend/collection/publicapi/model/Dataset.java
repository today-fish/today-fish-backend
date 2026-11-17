package com.todayfish.backend.collection.publicapi.model;

// 수집할 데이터셋과 제공기관의 관계를 나타낸다.
public enum Dataset {
    AT(Provider.AT),
    KMI(Provider.KMI);

    // 이 데이터셋을 제공하는 기관이다.
    private final Provider provider;

    Dataset(Provider provider) {
        this.provider = provider;
    }

    // 데이터셋의 제공기관을 반환한다.
    public Provider provider() {
        return provider;
    }
}
