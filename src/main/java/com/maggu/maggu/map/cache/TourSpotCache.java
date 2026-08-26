package com.maggu.maggu.map.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.maggu.maggu.map.client.TourSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TourSpotCache {

    private final Cache<String, TourSpot> cache;

    public boolean isEmpty() {
        return cache.estimatedSize() == 0;
    }

    public void putAll(List<TourSpot> spots) {
        Map<String, TourSpot> byContentId = spots.stream()
                .collect(Collectors.toMap(TourSpot::contentId, spot -> spot));
        cache.putAll(byContentId);
    }

    public void put(TourSpot spot) {
        cache.put(spot.contentId(), spot);
    }

    // bbox 내에 있는 스팟들을 가져옴
    public List<TourSpot> findInBbox(double minLng, double minLat, double maxLng, double maxLat) {
        return cache.asMap()
                .values()
                .stream()
                .filter(spot -> spot.mapX() <= maxLng && minLng <= spot.mapX()
                        && spot.mapY() <= maxLat && minLat <= spot.mapY())
                .toList();
    }
}
