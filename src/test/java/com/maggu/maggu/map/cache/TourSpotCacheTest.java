package com.maggu.maggu.map.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.client.TourSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TourSpotCacheTest {

    private TourSpotCache tourSpotCache;

    @BeforeEach
    void setUp() {
        Cache<String, TourSpot> cache = Caffeine.newBuilder().build();
        tourSpotCache = new TourSpotCache(cache);
    }

    @Nested
    @DisplayName("isEmpty")
    class IsEmpty {

        @Test
        @DisplayName("캐시에 아무것도 없으면 true를 반환한다")
        void returnsTrueWhenCacheIsEmpty() {
            assertThat(tourSpotCache.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("캐시에 값이 있으면 false를 반환한다")
        void returnsFalseWhenCacheHasValues() {
            tourSpotCache.putAll(List.of(spot("1", 127.0, 37.0)));

            assertThat(tourSpotCache.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("findInBbox")
    class FindInBbox {

        @Test
        @DisplayName("bbox 안에 있는 스팟만 반환한다")
        void returnsOnlySpotsWithinBbox() {
            TourSpot inside = spot("1", 127.05, 37.55);
            TourSpot outside = spot("2", 128.5, 36.0);
            tourSpotCache.putAll(List.of(inside, outside));

            List<TourSpot> result = tourSpotCache.findInBbox(126.8, 37.4, 127.2, 37.7);

            assertThat(result).containsExactly(inside);
        }

        @Test
        @DisplayName("bbox 경계값(최소/최대 좌표와 정확히 일치)도 포함한다")
        void includesSpotsExactlyOnBboxBoundary() {
            TourSpot onMinBoundary = spot("1", 126.8, 37.4);
            TourSpot onMaxBoundary = spot("2", 127.2, 37.7);
            tourSpotCache.putAll(List.of(onMinBoundary, onMaxBoundary));

            List<TourSpot> result = tourSpotCache.findInBbox(126.8, 37.4, 127.2, 37.7);

            assertThat(result).containsExactlyInAnyOrder(onMinBoundary, onMaxBoundary);
        }

        @Test
        @DisplayName("bbox 안에 스팟이 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoSpotsInBbox() {
            tourSpotCache.putAll(List.of(spot("1", 128.5, 36.0)));

            List<TourSpot> result = tourSpotCache.findInBbox(126.8, 37.4, 127.2, 37.7);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("캐시 자체가 비어있으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenCacheIsEmpty() {
            List<TourSpot> result = tourSpotCache.findInBbox(126.8, 37.4, 127.2, 37.7);

            assertThat(result).isEmpty();
        }
    }

    private TourSpot spot(String contentId, double mapX, double mapY) {
        return new TourSpot(contentId, ContentType.TOURIST_ATTRACTION, "테스트 스팟", mapX, mapY);
    }
}
