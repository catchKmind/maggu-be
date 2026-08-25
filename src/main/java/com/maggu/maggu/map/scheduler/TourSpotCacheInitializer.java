package com.maggu.maggu.map.scheduler;

/*
 * 1. 앱이 기동됐을 때 딱 한번 실행
 * 2. TourSpotCache.isEmpty()로 "이미 채워져 있으면 아무것도 안 함" 판단
 * 3. 비어있으면 TourServiceArea.values()를 순회하면서 각 지역에 대해 TourApiClient.findAllByArea(area) 호출
 * → 결과를 TourSpotCache.putAll(...)에 넘김
 * */

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.map.cache.TourSpotCache;
import com.maggu.maggu.map.client.TourApiClient;
import com.maggu.maggu.map.client.TourServiceArea;
import com.maggu.maggu.map.client.TourSpot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourSpotCacheInitializer {

    private final TourApiClient tourApiClient;
    private final TourSpotCache cache;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void init() {

        if (!cache.isEmpty()) {
            log.info("[MAP] TourSpot Cache에 이미 값 있음");
            return;
        }

        log.info("[MAP] TourSpot Cache에 적재 준비");

        for (TourServiceArea tourServiceArea : TourServiceArea.values()) {
            try {
                List<TourSpot> spots = tourApiClient.findAllByArea(tourServiceArea);
                cache.putAll(spots);
                log.info("[MAP] {} 지역 적재 완료({}건) ", tourServiceArea.getName(), spots.size());
            } catch (BusinessException e) {
                log.error("[MAP] {} 지역 TourSpot Cache 적재 실패", tourServiceArea.getName(), e);
            }
        }

        log.info("[MAP] TourSpot Cache 적재 완료");
    }
}
