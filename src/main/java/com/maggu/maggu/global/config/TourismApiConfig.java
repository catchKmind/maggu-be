package com.maggu.maggu.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TourismApiProperties.class)
public class TourismApiConfig {

    @Bean
    @Qualifier("korTourApiRestClient")
    public RestClient korTourApiRestClient(TourismApiProperties properties) {
        return buildClient(properties.baseUrl(), properties);
    }

    @Bean
    @Qualifier("engTourApiRestClient")
    public RestClient engTourApiRestClient(TourismApiProperties properties) {
        return buildClient(properties.engBaseUrl(), properties);
    }

    public RestClient tourismApiRestClient(TourismApiProperties properties) {
        return korTourApiRestClient(properties);
    }

    private RestClient buildClient(String baseUrl, TourismApiProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
