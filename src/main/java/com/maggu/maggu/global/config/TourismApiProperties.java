package com.maggu.maggu.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

// tourism-api 바인딩용 @ConfigurationProperties record
@ConfigurationProperties(prefix = "tourism-api")
public record TourismApiProperties(
        String baseUrl,
        @DefaultValue("http://apis.data.go.kr/B551011/EngService2") String engBaseUrl,
        String serviceKey,
        Duration connectTimeout,
        Duration readTimeout
) {
}
