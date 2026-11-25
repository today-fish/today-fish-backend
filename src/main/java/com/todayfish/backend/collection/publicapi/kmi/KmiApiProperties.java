package com.todayfish.backend.collection.publicapi.kmi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("collection.kmi")
public record KmiApiProperties(
        @NotNull URI endPoint,
        @NotBlank String serviceKey,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
}
