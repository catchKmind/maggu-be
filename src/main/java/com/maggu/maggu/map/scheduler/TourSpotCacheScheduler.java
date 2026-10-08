package com.maggu.maggu.map.scheduler;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class TourSpotCacheScheduler {

    private final TourApiClient tourApiClient;
    private final TourSpotCache tourSpotCache;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void init() {
        log.info("[MAP] TourSpotCache 적재 준비");
        for (AppLocale locale : AppLocale.values()) {
            init(locale);
        }
        log.info("[MAP] TourSpotCache 적재 완료");
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void refreshAt3Am() {
        log.info("[MAP] TourSpotCache 배치 준비");
        for (AppLocale locale : AppLocale.values()) {
            refresh(locale);
        }
        log.info("[MAP] TourSpotCache 배치 완료");
    }

    private void init(AppLocale locale) {
        if (!tourSpotCache.isEmpty(locale)) {
            log.info("[MAP] TourSpotCache에 이미 값 있음, locale={}", locale);
            return;
        }
        refresh(locale);
    }

    private void refresh(AppLocale locale) {
        for (TourServiceArea tourServiceArea : TourServiceArea.values()) {
            try {
                List<TourSpot> spots = tourApiClient.findAllByArea(tourServiceArea, locale);
                tourSpotCache.putAll(locale, spots);
                log.info("[MAP] {} 지역 적재 완료({}건) locale={}", tourServiceArea.getName(), spots.size(), locale);
            } catch (BusinessException e) {
                log.error("[MAP] {} 지역 TourSpotCache 적재 실패 locale={}", tourServiceArea.getName(), locale, e);
            }
        }
    }
}
