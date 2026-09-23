package com.maggu.maggu.map.scheduler;

/*
 * 1. 앱이 기동됐을 때 딱 한번 실행
 * 2. TourSpotCache.isEmpty(locale)로 언어별 적재 여부 판단
 * 3. 비어있으면 TourServiceArea.values()를 순회하면서 각 지역에 대해 TourApiClient.findAllByArea(area, locale) 호출
 * → 결과를 TourSpotCache.putAll(locale, ...)에 넘김
 * */

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.map.cache.TourSpotCache;
import com.maggu.maggu.map.client.TourApiClient;
import com.maggu.maggu.map.client.TourServiceArea;
import com.maggu.maggu.map.client.TourSpot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class TourSpotCacheInitializer {

    private final TourApiClient tourApiClient;
    private final TourSpotCache cache;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void init() {
        for (AppLocale locale : AppLocale.values()) {
            init(locale);
        }
        log.info("[MAP] TourSpot Cache 적재 완료");
    }

    private void init(AppLocale locale) {
        if (!cache.isEmpty(locale)) {
            log.info("[MAP] TourSpot Cache에 이미 값 있음 locale={}", locale);
            return;
        }

        log.info("[MAP] TourSpot Cache에 적재 준비 locale={}", locale);

        for (TourServiceArea tourServiceArea : TourServiceArea.values()) {
            try {
                List<TourSpot> spots = tourApiClient.findAllByArea(tourServiceArea, locale);
                cache.putAll(locale, spots);
                log.info("[MAP] {} 지역 적재 완료({}건) locale={}", tourServiceArea.getName(), spots.size(), locale);
            } catch (BusinessException e) {
                log.error("[MAP] {} 지역 TourSpot Cache 적재 실패 locale={}", tourServiceArea.getName(), locale, e);
            }
        }
    }
}
