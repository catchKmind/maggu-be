package com.maggu.maggu.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// tourism-api 바인딩용 @ConfigurationProperties record
@ConfigurationProperties(prefix = "tourism-api")
public record TourismApiProperties(
        String baseUrl,
        String serviceKey,
        Duration connectTimeout,
        Duration readTimeout
) {
}
