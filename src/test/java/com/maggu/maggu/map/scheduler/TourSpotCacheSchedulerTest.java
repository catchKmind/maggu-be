package com.maggu.maggu.map.scheduler;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.map.cache.TourSpotCache;
import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.client.TourApiClient;
import com.maggu.maggu.map.client.TourServiceArea;
import com.maggu.maggu.map.client.TourSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TourSpotCacheSchedulerTest {

    private static final double GB_LNG = 128.6;
    private static final double GB_LAT = 36.0;
    private static final double GN_LNG = 128.3;
    private static final double GN_LAT = 35.2;

    @Mock
    private TourApiClient tourApiClient;

    private TourSpotCache tourSpotCache;
    private TourSpotCacheScheduler scheduler;

    @BeforeEach
    void setUp() {
        tourSpotCache = new TourSpotCache(Caffeine.newBuilder().build());
        scheduler = new TourSpotCacheScheduler(tourApiClient, tourSpotCache);
    }

    @Nested
    @DisplayName("init - 앱 기동 시 적재")
    class Init {

        @Test
        @DisplayName("캐시가 비어 있으면 모든 언어 × 모든 지역을 조회해 적재한다")
        void loadsAllLocalesAndAreasWhenCacheIsEmpty() {
            givenAreaResponse(TourServiceArea.GB, AppLocale.KO, spot("GB-1", "경북 장소", GB_LNG, GB_LAT));
            givenAreaResponse(TourServiceArea.GN, AppLocale.KO, spot("GN-1", "경남 장소", GN_LNG, GN_LAT));
            givenAreaResponse(TourServiceArea.GB, AppLocale.EN, spot("GB-1", "GB spot", GB_LNG, GB_LAT));
            givenAreaResponse(TourServiceArea.GN, AppLocale.EN, spot("GN-1", "GN spot", GN_LNG, GN_LAT));

            scheduler.init();

            assertThat(titlesIn(AppLocale.KO)).containsExactlyInAnyOrder("경북 장소", "경남 장소");
            assertThat(titlesIn(AppLocale.EN)).containsExactlyInAnyOrder("GB spot", "GN spot");
        }

        @Test
        @DisplayName("이미 값이 있는 언어는 TourAPI를 호출하지 않고 건너뛴다")
        void skipsLocaleThatAlreadyHasValues() {
            tourSpotCache.putAll(AppLocale.KO, List.of(spot("GB-1", "기존 장소", GB_LNG, GB_LAT)));
            givenAreaResponse(TourServiceArea.GB, AppLocale.EN, spot("GB-1", "GB spot", GB_LNG, GB_LAT));
            givenAreaResponse(TourServiceArea.GN, AppLocale.EN, spot("GN-1", "GN spot", GN_LNG, GN_LAT));

            scheduler.init();

            verify(tourApiClient, never()).findAllByArea(any(), eq(AppLocale.KO));
            assertThat(titlesIn(AppLocale.KO)).containsExactly("기존 장소");
            assertThat(titlesIn(AppLocale.EN)).containsExactlyInAnyOrder("GB spot", "GN spot");
        }

        @Test
        @DisplayName("한 지역 조회가 실패해도 예외를 전파하지 않고 나머지 지역은 적재한다")
        void loadsOtherAreasWhenOneAreaFails() {
            givenAreaFailure(TourServiceArea.GB, AppLocale.KO);
            givenAreaResponse(TourServiceArea.GN, AppLocale.KO, spot("GN-1", "경남 장소", GN_LNG, GN_LAT));
            givenAreaResponse(TourServiceArea.GB, AppLocale.EN, spot("GB-1", "GB spot", GB_LNG, GB_LAT));
            givenAreaResponse(TourServiceArea.GN, AppLocale.EN, spot("GN-1", "GN spot", GN_LNG, GN_LAT));

            assertThatCode(() -> scheduler.init()).doesNotThrowAnyException();

            assertThat(titlesIn(AppLocale.KO)).containsExactly("경남 장소");
            assertThat(titlesIn(AppLocale.EN)).containsExactlyInAnyOrder("GB spot", "GN spot");
        }
    }

    @Nested
    @DisplayName("refreshAt3Am - 매일 새벽 3시 배치")
    class DailyRefresh {

        @Test
        @DisplayName("캐시에 이미 값이 있어도 모든 언어 × 모든 지역을 다시 조회한다")
        void callsTourApiEvenWhenCacheHasValues() {
            tourSpotCache.putAll(AppLocale.KO, List.of(spot("GB-1", "기존 장소", GB_LNG, GB_LAT)));
            tourSpotCache.putAll(AppLocale.EN, List.of(spot("GB-1", "old spot", GB_LNG, GB_LAT)));
            givenAllAreasReturnEmpty();

            scheduler.refreshAt3Am();

            verify(tourApiClient, times(TourServiceArea.values().length))
                    .findAllByArea(any(), eq(AppLocale.KO));
            verify(tourApiClient, times(TourServiceArea.values().length))
                    .findAllByArea(any(), eq(AppLocale.EN));
        }

        @Test
        @DisplayName("같은 contentId는 새 응답으로 덮어써서 변경된 정보가 반영된다")
        void overwritesExistingSpotWithLatestResponse() {
            tourSpotCache.putAll(AppLocale.KO, List.of(spot("GB-1", "옛 이름", GB_LNG, GB_LAT)));
            givenAllAreasReturnEmpty();
            givenAreaResponse(TourServiceArea.GB, AppLocale.KO, spot("GB-1", "새 이름", GB_LNG, GB_LAT));

            scheduler.refreshAt3Am();

            assertThat(titlesIn(AppLocale.KO)).containsExactly("새 이름");
        }

        @Test
        @DisplayName("응답에서 빠진 장소는 지우지 않는다(정리는 TTL 만료에 맡긴다)")
        void keepsSpotMissingFromResponse() {
            tourSpotCache.putAll(AppLocale.KO, List.of(spot("GB-OLD", "폐업한 장소", GB_LNG, GB_LAT)));
            givenAllAreasReturnEmpty();
            givenAreaResponse(TourServiceArea.GB, AppLocale.KO, spot("GB-NEW", "새 장소", GB_LNG, GB_LAT));

            scheduler.refreshAt3Am();

            assertThat(titlesIn(AppLocale.KO)).containsExactlyInAnyOrder("폐업한 장소", "새 장소");
        }

        @Test
        @DisplayName("한 지역 조회가 실패하면 그 지역은 이전 값을 유지하고 나머지 지역은 갱신된다")
        void keepsPreviousValuesOfFailedAreaAndRefreshesOthers() {
            tourSpotCache.putAll(AppLocale.KO, List.of(
                    spot("GB-1", "경북 옛 이름", GB_LNG, GB_LAT),
                    spot("GN-1", "경남 옛 이름", GN_LNG, GN_LAT)));
            givenAllAreasReturnEmpty();
            givenAreaFailure(TourServiceArea.GB, AppLocale.KO);
            givenAreaResponse(TourServiceArea.GN, AppLocale.KO, spot("GN-1", "경남 새 이름", GN_LNG, GN_LAT));

            assertThatCode(() -> scheduler.refreshAt3Am()).doesNotThrowAnyException();

            assertThat(titlesIn(AppLocale.KO)).containsExactlyInAnyOrder("경북 옛 이름", "경남 새 이름");
        }
    }

    private void givenAreaResponse(TourServiceArea area, AppLocale locale, TourSpot... spots) {
        given(tourApiClient.findAllByArea(area, locale)).willReturn(List.of(spots));
    }

    private void givenAreaFailure(TourServiceArea area, AppLocale locale) {
        given(tourApiClient.findAllByArea(area, locale))
                .willThrow(new BusinessException(ErrorCode.TOURISM_API_QUOTA_EXCEEDED));
    }

    private void givenAllAreasReturnEmpty() {
        given(tourApiClient.findAllByArea(any(), any())).willReturn(List.of());
    }

    private List<String> titlesIn(AppLocale locale) {
        return tourSpotCache.findInBbox(locale, -180, -90, 180, 90).stream()
                .map(TourSpot::title)
                .toList();
    }

    private TourSpot spot(String contentId, String title, double mapX, double mapY) {
        return new TourSpot(contentId, ContentType.TOURIST_ATTRACTION, title, mapX, mapY);
    }
}
