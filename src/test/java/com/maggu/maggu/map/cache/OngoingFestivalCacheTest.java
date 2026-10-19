package com.maggu.maggu.map.cache;

import com.maggu.maggu.global.entity.enums.AppLocale;
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
            cache.replaceRegion(AppLocale.KO, TourServiceArea.GB, List.of(festival("111")));

            assertThat(cache.isOngoing(AppLocale.KO, "111")).isTrue();
        }

        @Test
        @DisplayName("캐시에 없는 contentId는 false를 반환한다")
        void returnsFalseWhenContentIdIsNotCached() {
            cache.replaceRegion(AppLocale.KO, TourServiceArea.GB, List.of(festival("111")));

            assertThat(cache.isOngoing(AppLocale.KO, "999")).isFalse();
        }

        @Test
        @DisplayName("아무것도 적재되지 않은 초기 상태에서는 false를 반환한다")
        void returnsFalseWhenCacheIsEmpty() {
            assertThat(cache.isOngoing(AppLocale.KO, "111")).isFalse();
        }

        @Test
        @DisplayName("다른 언어로 적재된 contentId는 false를 반환한다(KO/EN contentId 체계가 다름)")
        void returnsFalseForContentIdLoadedInOtherLocale() {
            cache.replaceRegion(AppLocale.KO, TourServiceArea.GB, List.of(festival("111")));

            assertThat(cache.isOngoing(AppLocale.EN, "111")).isFalse();
        }

        @Test
        @DisplayName("한 번도 적재되지 않은 언어로 조회하면 예외 없이 false를 반환한다")
        void returnsFalseForLocaleNeverLoaded() {
            cache.replaceRegion(AppLocale.KO, TourServiceArea.GB, List.of(festival("111")));
            cache.replaceRegion(AppLocale.KO, TourServiceArea.GN, List.of(festival("222")));

            assertThat(cache.isOngoing(AppLocale.EN, "999")).isFalse();
        }
    }

    @Nested
    @DisplayName("replaceRegion")
    class ReplaceRegion {

        @Test
        @DisplayName("한 지역을 갱신해도 다른 지역의 기존 데이터는 그대로 유지된다")
        void keepsOtherRegionsDataWhenOneRegionIsReplaced() {
            cache.replaceRegion(AppLocale.KO, TourServiceArea.GB, List.of(festival("111")));

            cache.replaceRegion(AppLocale.KO, TourServiceArea.GN, List.of(festival("222")));

            assertThat(cache.isOngoing(AppLocale.KO, "111")).isTrue();
            assertThat(cache.isOngoing(AppLocale.KO, "222")).isTrue();
        }

        @Test
        @DisplayName("같은 지역을 다시 갱신하면 그 지역의 이전 값은 사라지고 새 값만 남는다")
        void oldDataForSameRegionIsFullyReplaced() {
            cache.replaceRegion(AppLocale.KO, TourServiceArea.GB, List.of(festival("111")));

            cache.replaceRegion(AppLocale.KO, TourServiceArea.GB, List.of(festival("222")));

            assertThat(cache.isOngoing(AppLocale.KO, "111")).isFalse();
            assertThat(cache.isOngoing(AppLocale.KO, "222")).isTrue();
        }

        @Test
        @DisplayName("다른 언어의 같은 지역을 갱신해도 기존 언어의 값은 덮어써지지 않는다")
        void doesNotOverwriteOtherLocaleOfSameRegion() {
            cache.replaceRegion(AppLocale.KO, TourServiceArea.GB, List.of(festival("111"), festival("112")));

            cache.replaceRegion(AppLocale.EN, TourServiceArea.GB, List.of(festival("900")));

            assertThat(cache.isOngoing(AppLocale.KO, "111")).isTrue();
            assertThat(cache.isOngoing(AppLocale.KO, "112")).isTrue();
            assertThat(cache.isOngoing(AppLocale.EN, "900")).isTrue();
            assertThat(cache.isOngoing(AppLocale.KO, "900")).isFalse();
        }

        @Test
        @DisplayName("한 언어의 지역을 갱신해도 다른 언어의 다른 지역 데이터는 그대로 유지된다")
        void keepsOtherLocaleAndRegionWhenOneIsReplaced() {
            cache.replaceRegion(AppLocale.KO, TourServiceArea.GN, List.of(festival("222")));
            cache.replaceRegion(AppLocale.EN, TourServiceArea.GB, List.of(festival("900")));

            cache.replaceRegion(AppLocale.EN, TourServiceArea.GB, List.of(festival("901")));

            assertThat(cache.isOngoing(AppLocale.KO, "222")).isTrue();
            assertThat(cache.isOngoing(AppLocale.EN, "900")).isFalse();
            assertThat(cache.isOngoing(AppLocale.EN, "901")).isTrue();
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
