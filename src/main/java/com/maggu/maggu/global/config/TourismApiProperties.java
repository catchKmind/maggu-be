package com.maggu.maggu.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/*
 * tourism-api.base-url/tourism-api.service-key 바인딩용 @ConfigurationProperties record
 * */
@ConfigurationProperties(prefix = "tourism-api")
public record TourismApiProperties(String baseUrl, String serviceKey) {
}
