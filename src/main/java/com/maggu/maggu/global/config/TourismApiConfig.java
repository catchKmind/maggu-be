package com.maggu.maggu.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/*
 * RestClient 빈 등록 (baseUrl만 고정, serviceKey는 호출부에서 요청마다 쿼리파라미터로 조립)
 * */
@Configuration
@EnableConfigurationProperties(TourismApiProperties.class)
public class TourismApiConfig {

    @Bean
    public RestClient tourismApiRestClient(TourismApiProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
