package com.maggu.maggu.map.cache;

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
    private final AtomicReference<Map<TourServiceArea, Map<String, FestivalSpot>>> cache =
            new AtomicReference<>(Map.of());

    public void replaceRegion(TourServiceArea area, List<FestivalSpot> festivals) {
        Map<String, FestivalSpot> byContentId = festivals.stream()
                .collect(Collectors.toMap(FestivalSpot::contentId, spot -> spot));

        cache.updateAndGet(current -> {
            Map<TourServiceArea, Map<String, FestivalSpot>> updated = new EnumMap<>(TourServiceArea.class);
            updated.putAll(current); // 기존 지역별 데이터 복사
            updated.put(area, byContentId); // 이 지역만 새 값으로 교체
            return Map.copyOf(updated); // 새로운 불변 맵 반환(current 건드지리 않음)
        });
    }

    public boolean isOngoing(String contentId) {
        return cache.get().values().stream()
                .anyMatch(regionMap -> regionMap.containsKey(contentId));
    }
}
