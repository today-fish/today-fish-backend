package com.todayfish.backend.collection.publicapi.at;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("collection.at")
public record AtApiProperties(
        @NotNull URI endPoint,
        @NotBlank String serviceKey,
        @Positive int pageSize,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
}
