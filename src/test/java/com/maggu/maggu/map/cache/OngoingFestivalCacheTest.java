package com.maggu.maggu.map.cache;

import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.client.FestivalSpot;
import com.maggu.maggu.map.client.TourServiceArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OngoingFestivalCacheTest {

    private OngoingFestivalCache cache;

    @BeforeEach
    void setUp() {
        cache = new OngoingFestivalCache();
    }

    @Nested
    @DisplayName("isOngoing")
    class IsOngoing {

        @Test
        @DisplayName("캐시에 있는 contentId는 true를 반환한다")
        void returnsTrueWhenContentIdIsCached() {
            cache.replaceRegion(TourServiceArea.GB, List.of(festival("111")));

            assertThat(cache.isOngoing("111")).isTrue();
        }

        @Test
        @DisplayName("캐시에 없는 contentId는 false를 반환한다")
        void returnsFalseWhenContentIdIsNotCached() {
            cache.replaceRegion(TourServiceArea.GB, List.of(festival("111")));

            assertThat(cache.isOngoing("999")).isFalse();
        }

        @Test
        @DisplayName("아무것도 적재되지 않은 초기 상태에서는 false를 반환한다")
        void returnsFalseWhenCacheIsEmpty() {
            assertThat(cache.isOngoing("111")).isFalse();
        }
    }

    @Nested
    @DisplayName("replaceRegion")
    class ReplaceRegion {

        @Test
        @DisplayName("한 지역을 갱신해도 다른 지역의 기존 데이터는 그대로 유지된다")
        void keepsOtherRegionsDataWhenOneRegionIsReplaced() {
            cache.replaceRegion(TourServiceArea.GB, List.of(festival("111")));

            cache.replaceRegion(TourServiceArea.GN, List.of(festival("222")));

            assertThat(cache.isOngoing("111")).isTrue();
            assertThat(cache.isOngoing("222")).isTrue();
        }

        @Test
        @DisplayName("같은 지역을 다시 갱신하면 그 지역의 이전 값은 사라지고 새 값만 남는다")
        void oldDataForSameRegionIsFullyReplaced() {
            cache.replaceRegion(TourServiceArea.GB, List.of(festival("111")));

            cache.replaceRegion(TourServiceArea.GB, List.of(festival("222")));

            assertThat(cache.isOngoing("111")).isFalse();
            assertThat(cache.isOngoing("222")).isTrue();
        }
    }

    private FestivalSpot festival(String contentId) {
        return FestivalSpot.builder()
                .contentId(contentId)
                .contentType(ContentType.FESTIVAL)
                .title("테스트 축제")
                .eventStartDate(LocalDate.of(2026, 9, 18))
                .eventEndDate(LocalDate.of(2026, 9, 20))
                .mapX(127.05)
                .mapY(37.55)
                .build();
    }
}
