package com.maggu.maggu.map.scheduler;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.map.cache.OngoingFestivalCache;
import com.maggu.maggu.map.client.FestivalSpot;
import com.maggu.maggu.map.client.TourApiClient;
import com.maggu.maggu.map.client.TourServiceArea;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class OngoingFestivalScheduler {

    private final TourApiClient tourApiClient;
    private final OngoingFestivalCache cache;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void init() {
        callSearchFestivalAPI();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void callSearchFestivalAPIAtMidNight() {
        callSearchFestivalAPI();
    }

    private void callSearchFestivalAPI() {
        log.info("[MAP] OngoingFestival Cache에 적재 준비");
        for (TourServiceArea tourServiceArea : TourServiceArea.values()) {
            try {
                List<FestivalSpot> spots = tourApiClient.searchFestival(tourServiceArea);
                cache.replaceRegion(tourServiceArea, spots);
                log.info("[MAP] {} 지역 적재 완료({}건) ", tourServiceArea.getName(), spots.size());
            } catch (BusinessException e) {
                log.error("[MAP] {} 지역 OngoingFestival Cache 적재 실패", tourServiceArea.getName(), e);
            }
        }
        log.info("[MAP] OngoingFestival Cache 적재 완료");
    }
}
