package com.maggu.maggu.map.cache;

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.map.client.FestivalSpot;
import com.maggu.maggu.map.client.TourServiceArea;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OngoingFestivalCache {
    private final AtomicReference<Map<AppLocale, Map<TourServiceArea, Map<String, FestivalSpot>>>> cache =
            new AtomicReference<>(Map.of());

    public void replaceRegion(AppLocale locale, TourServiceArea area, List<FestivalSpot> festivals) {
        Map<String, FestivalSpot> byContentId = festivals.stream()
                .collect(Collectors.toMap(FestivalSpot::contentId, spot -> spot));

        cache.updateAndGet(current -> {
            Map<TourServiceArea, Map<String, FestivalSpot>> regions = new EnumMap<>(TourServiceArea.class);
            regions.putAll(current.getOrDefault(locale, Map.of())); // 기존 지역별 데이터 복사
            regions.put(area, byContentId); // 이 지역만 새 값으로 교체

            Map<AppLocale, Map<TourServiceArea, Map<String, FestivalSpot>>> updated = new EnumMap<>(AppLocale.class);
            updated.putAll(current);
            updated.put(locale, Map.copyOf(regions));

            return Map.copyOf(updated); // 새로운 불변 맵 반환(current 건드지리 않음)
        });
    }

    public boolean isOngoing(AppLocale locale, String contentId) {
        return cache.get().getOrDefault(locale, Map.of()).values().stream()
                .anyMatch(regionMap -> regionMap.containsKey(contentId));
    }
}
