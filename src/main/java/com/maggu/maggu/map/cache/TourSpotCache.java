package com.maggu.maggu.map.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.map.client.TourSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TourSpotCache {

    private static final String KEY_SEPARATOR = ":";

    private final Cache<String, TourSpot> cache;

    public boolean isEmpty() {
        return isEmpty(AppLocale.KO);
    }

    public boolean isEmpty(AppLocale locale) {
        String prefix = keyPrefix(locale);
        return cache.asMap().keySet().stream().noneMatch(key -> key.startsWith(prefix));
    }

    public void putAll(List<TourSpot> spots) {
        putAll(AppLocale.KO, spots);
    }

    public void putAll(AppLocale locale, List<TourSpot> spots) {
        Map<String, TourSpot> byContentId = spots.stream()
                .collect(Collectors.toMap(spot -> key(locale, spot.contentId()), spot -> spot));
        cache.putAll(byContentId);
    }

    public void put(TourSpot spot) {
        put(AppLocale.KO, spot);
    }

    public void put(AppLocale locale, TourSpot spot) {
        cache.put(key(locale, spot.contentId()), spot);
    }

    public List<TourSpot> findInBbox(double minLng, double minLat, double maxLng, double maxLat) {
        return findInBbox(AppLocale.KO, minLng, minLat, maxLng, maxLat);
    }

    public List<TourSpot> findInBbox(AppLocale locale, double minLng, double minLat, double maxLng, double maxLat) {
        String prefix = keyPrefix(locale);
        return cache.asMap()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue)
                .filter(spot -> spot.mapX() <= maxLng && minLng <= spot.mapX()
                        && spot.mapY() <= maxLat && minLat <= spot.mapY())
                .toList();
    }

    public List<TourSpot> findByKeyword(String keyword, int limit) {
        return findByKeyword(AppLocale.KO, keyword, limit);
    }

    public List<TourSpot> findByKeyword(AppLocale locale, String keyword, int limit) {
        String prefix = keyPrefix(locale);
        return cache.asMap()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue)
                .filter(spot -> spot.title().contains(keyword))
                .limit(limit)
                .toList();
    }

    private String key(AppLocale locale, String contentId) {
        return keyPrefix(locale) + contentId;
    }

    private String keyPrefix(AppLocale locale) {
        AppLocale resolved = locale == null ? AppLocale.KO : locale;
        return resolved.name() + KEY_SEPARATOR;
    }
}
