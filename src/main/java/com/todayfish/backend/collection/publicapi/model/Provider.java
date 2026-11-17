package com.todayfish.backend.collection.publicapi.model;

// 공공 API를 제공하는 기관을 나타낸다.
public enum Provider {
    AT("한국농수산식품유통공사"),
    KMI("한국해양수산개발원");

    // 기관을 표시할 때 사용하는 이름이다.
    private final String displayName ;

    Provider(String displayName) {
        this.displayName = displayName;
    }

    // 기관의 표시 이름을 반환한다.
    public String displayName() {
        return this.displayName;
    }
}

