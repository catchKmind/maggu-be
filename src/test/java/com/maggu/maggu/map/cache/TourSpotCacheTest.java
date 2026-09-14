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

    @Nested
    @DisplayName("findByKeyword")
    class FindByKeyword {

        @Test
        @DisplayName("제목에 키워드가 포함된 스팟을 반환한다")
        void returnsSpotsMatchingTitle() {
            TourSpot matched = spot("1", "해운대해수욕장", 129.16, 35.16);
            TourSpot notMatched = spot("2", "남산타워", 127.0, 37.5);
            tourSpotCache.putAll(List.of(matched, notMatched));

            List<TourSpot> result = tourSpotCache.findByKeyword("해운대", 10);

            assertThat(result).containsExactly(matched);
        }

        @Test
        @DisplayName("제목에 키워드가 없으면 결과에서 제외한다")
        void excludesSpotsNotMatchingTitle() {
            TourSpot notMatched = spot("1", "남산타워", 127.0, 37.5);
            tourSpotCache.putAll(List.of(notMatched));

            List<TourSpot> result = tourSpotCache.findByKeyword("해운대", 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("매칭되지 않는 스팟이 섞여 있어도 매칭된 것만 limit 개수만큼 반환한다")
        void limitsResultCount() {
            // 매칭 안 되는 스팟을 매칭되는 스팟보다 먼저/더 많이 섞어 넣어서,
            // filter보다 limit을 먼저 적용하는 회귀(매칭 스팟이 잘려나가는 버그)가 생기면 이 테스트가 깨진다.
            TourSpot notMatched1 = spot("1", "남산타워", 127.0, 37.5);
            TourSpot matched1 = spot("2", "해운대해수욕장", 129.16, 35.16);
            TourSpot notMatched2 = spot("3", "경복궁", 126.9, 37.5);
            TourSpot matched2 = spot("4", "해운대시장", 129.17, 35.17);
            TourSpot matched3 = spot("5", "해운대해변열차", 129.18, 35.18);
            tourSpotCache.putAll(List.of(notMatched1, matched1, notMatched2, matched2, matched3));

            List<TourSpot> result = tourSpotCache.findByKeyword("해운대", 2);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(spot -> spot.title().contains("해운대"));
        }
    }

    private TourSpot spot(String contentId, double mapX, double mapY) {
        return spot(contentId, "테스트 스팟", mapX, mapY);
    }

    private TourSpot spot(String contentId, String title, double mapX, double mapY) {
        return new TourSpot(contentId, ContentType.TOURIST_ATTRACTION, title, mapX, mapY);
    }
}
