package com.maggu.maggu.global.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maggu.maggu.map.client.TourSpot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class TourSpotCacheConfig {

    @Bean
    public Cache<String, TourSpot> tourSpotCaffeineCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofDays(7))
                .maximumSize(20_000)
                .build();
    }
}
