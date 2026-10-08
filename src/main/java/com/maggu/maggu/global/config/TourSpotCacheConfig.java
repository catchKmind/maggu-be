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
                .expireAfterWrite(Duration.ofHours(50))
                .maximumSize(40_000)
                .build();
    }
}
