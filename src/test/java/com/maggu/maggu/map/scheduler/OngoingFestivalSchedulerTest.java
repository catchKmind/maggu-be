package com.maggu.maggu.map.scheduler;

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.map.cache.OngoingFestivalCache;
import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.client.FestivalSpot;
import com.maggu.maggu.map.client.TourApiClient;
import com.maggu.maggu.map.client.TourServiceArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OngoingFestivalSchedulerTest {

    @Mock
    private TourApiClient tourApiClient;

    private OngoingFestivalCache cache;
    private OngoingFestivalScheduler scheduler;

    @BeforeEach
    void setUp() {
        cache = new OngoingFestivalCache();
        scheduler = new OngoingFestivalScheduler(tourApiClient, cache);
    }

    @Nested
    @DisplayName("init - 앱 기동 시 적재")
    class Init {

        @Test
        @DisplayName("모든 언어 × 모든 지역을 조회해 언어별로 적재한다")
        void loadsAllLocalesAndAreas() {
            givenFestivalResponse(TourServiceArea.GB, AppLocale.KO, festival("KO-GB"));
            givenFestivalResponse(TourServiceArea.GN, AppLocale.KO, festival("KO-GN"));
            givenFestivalResponse(TourServiceArea.GB, AppLocale.EN, festival("EN-GB"));
            givenFestivalResponse(TourServiceArea.GN, AppLocale.EN, festival("EN-GN"));

            scheduler.init();

            assertThat(cache.isOngoing(AppLocale.KO, "KO-GB")).isTrue();
            assertThat(cache.isOngoing(AppLocale.KO, "KO-GN")).isTrue();
            assertThat(cache.isOngoing(AppLocale.EN, "EN-GB")).isTrue();
            assertThat(cache.isOngoing(AppLocale.EN, "EN-GN")).isTrue();
        }

        @Test
        @DisplayName("EN을 나중에 적재해도 같은 지역의 KO 값이 덮어써지지 않는다")
        void enLoadDoesNotOverwriteKoOfSameArea() {
            givenAllAreasReturnEmpty();
            givenFestivalResponse(TourServiceArea.GB, AppLocale.KO, festival("111"), festival("112"));
            givenFestivalResponse(TourServiceArea.GB, AppLocale.EN, festival("900"));

            scheduler.init();

            assertThat(cache.isOngoing(AppLocale.KO, "111")).isTrue();
            assertThat(cache.isOngoing(AppLocale.KO, "112")).isTrue();
            assertThat(cache.isOngoing(AppLocale.EN, "900")).isTrue();
            assertThat(cache.isOngoing(AppLocale.KO, "900")).isFalse();
        }

        @Test
        @DisplayName("한 언어·지역 조회가 실패해도 예외를 전파하지 않고 나머지는 적재한다")
        void loadsOthersWhenOneLocaleAreaFails() {
            givenFestivalResponse(TourServiceArea.GB, AppLocale.KO, festival("KO-GB"));
            givenFestivalResponse(TourServiceArea.GN, AppLocale.KO, festival("KO-GN"));
            givenFestivalFailure(TourServiceArea.GB, AppLocale.EN);
            givenFestivalResponse(TourServiceArea.GN, AppLocale.EN, festival("EN-GN"));

            assertThatCode(() -> scheduler.init()).doesNotThrowAnyException();

            assertThat(cache.isOngoing(AppLocale.KO, "KO-GB")).isTrue();
            assertThat(cache.isOngoing(AppLocale.KO, "KO-GN")).isTrue();
            assertThat(cache.isOngoing(AppLocale.EN, "EN-GN")).isTrue();
        }
    }

    @Nested
    @DisplayName("callSearchFestivalAPIAtMidNight - 매일 자정 배치")
    class MidnightRefresh {

        @Test
        @DisplayName("모든 언어 × 모든 지역을 다시 조회한다")
        void callsTourApiForAllLocalesAndAreas() {
            givenAllAreasReturnEmpty();

            scheduler.callSearchFestivalAPIAtMidNight();

            verify(tourApiClient, times(TourServiceArea.values().length))
                    .searchFestival(any(), eq(AppLocale.KO));
            verify(tourApiClient, times(TourServiceArea.values().length))
                    .searchFestival(any(), eq(AppLocale.EN));
        }

        @Test
        @DisplayName("한 언어·지역 조회가 실패하면 그 칸만 이전 값을 유지하고 나머지는 갱신된다")
        void keepsPreviousValuesOfFailedLocaleAreaAndRefreshesOthers() {
            cache.replaceRegion(AppLocale.KO, TourServiceArea.GB, List.of(festival("KO-OLD")));
            cache.replaceRegion(AppLocale.EN, TourServiceArea.GB, List.of(festival("EN-OLD")));
            givenAllAreasReturnEmpty();
            givenFestivalFailure(TourServiceArea.GB, AppLocale.EN);
            givenFestivalResponse(TourServiceArea.GB, AppLocale.KO, festival("KO-NEW"));

            assertThatCode(() -> scheduler.callSearchFestivalAPIAtMidNight()).doesNotThrowAnyException();

            assertThat(cache.isOngoing(AppLocale.KO, "KO-OLD")).isFalse();
            assertThat(cache.isOngoing(AppLocale.KO, "KO-NEW")).isTrue();
            assertThat(cache.isOngoing(AppLocale.EN, "EN-OLD")).isTrue();
        }
    }

    private void givenFestivalResponse(TourServiceArea area, AppLocale locale, FestivalSpot... festivals) {
        given(tourApiClient.searchFestival(area, locale)).willReturn(List.of(festivals));
    }

    private void givenFestivalFailure(TourServiceArea area, AppLocale locale) {
        given(tourApiClient.searchFestival(area, locale))
                .willThrow(new BusinessException(ErrorCode.TOURISM_API_QUOTA_EXCEEDED));
    }

    private void givenAllAreasReturnEmpty() {
        given(tourApiClient.searchFestival(any(), any())).willReturn(List.of());
    }

    private FestivalSpot festival(String contentId) {
        return FestivalSpot.builder()
                .contentId(contentId)
                .contentType(ContentType.FESTIVAL)
                .title("테스트 축제")
                .eventStartDate(LocalDate.of(2026, 9, 18))
                .eventEndDate(LocalDate.of(2026, 9, 20))
                .mapX(128.6)
                .mapY(36.0)
                .build();
    }
}
