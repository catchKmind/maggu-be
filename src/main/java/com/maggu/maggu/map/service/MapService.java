package com.maggu.maggu.map.service;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.map.cache.TourSpotCache;
import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.client.TourApiClient;
import com.maggu.maggu.map.client.TourSpot;
import com.maggu.maggu.map.dto.*;
import com.maggu.maggu.map.enums.MapCategory;
import com.maggu.maggu.post.repository.MapPostProjection;
import com.maggu.maggu.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapService {

    private final PostRepository postRepository;
    private final TourApiClient tourApiClient;
    private final TourSpotCache tourSpotCache;

    public MapSpotsResponse getMapSpots(double minLat, double minLng, double maxLat, double maxLng) {
        validateBbox(minLat, minLng, maxLat, maxLng);

        List<TourSpot> tourSpots = tourSpotCache.findInBbox(minLng, minLat, maxLng, maxLat);

        List<MapSpotFeature> features = tourSpots.stream()
                .map(this::toFeature)
                .toList();

        return MapSpotsResponse.of(features);
    }

    // bbox 유효성 검증 후 그 안의 게시글을 조회해 GeoJSON FeatureCollection으로 변환해 반환
    public MapPostsResponse getMapPosts(double minLat, double minLng, double maxLat, double maxLng,
                                        MapCategory category) {
        validateBbox(minLat, minLng, maxLat, maxLng);

        List<MapPostProjection> projections = postRepository.findInBbox(minLng, minLat, maxLng, maxLat);

        // 카테고리별 필터링
        if (category == MapCategory.POPULAR) {
            projections = sortByScrapCountDesc(projections);
        } else if (category != null && !projections.isEmpty()) {
            projections = filterByCategory(projections, category);
        }

        Map<String, Long> placePostCounts = countByTourismContentId(projections);

        List<MapPostFeature> features = projections.stream()
                .map(p -> toFeature(p, placePostCounts))
                .toList();

        return MapPostsResponse.of(features);
    }

    // 같은 tourismContentId를 공유하는 게시글 개수
    private Map<String, Long> countByTourismContentId(List<MapPostProjection> projections) {
        return projections.stream()
                .filter(p -> p.getTourismContentId() != null)
                .collect(Collectors.groupingBy(MapPostProjection::getTourismContentId, Collectors.counting()));
    }

    private List<MapPostProjection> sortByScrapCountDesc(List<MapPostProjection> projections) {
        return projections.stream()
                .sorted(Comparator.comparingInt(MapPostProjection::getScrapCount).reversed())
                .toList();
    }

    // 카테고리(FOOD/LANDMARK/STAY) 필터
    private List<MapPostProjection> filterByCategory(List<MapPostProjection> projections, MapCategory category) {

        Set<String> tourContentIds = projections.stream()
                .filter(p -> p.getTourismContentId() != null)
                .map(MapPostProjection::getTourismContentId)
                .collect(Collectors.toSet());

        // /detailCommon2 api 호출해서 contentTypeId를 얻어서 필터링에 활용
        // Map<contentId, ContentType>
        Map<String, ContentType> contentTypesByContentId = tourContentIds.stream()
                .map(id -> Map.entry(id, tourApiClient.findContentType(id)))
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));

        return projections.stream()
                .filter(p -> p.getTourismContentId() != null
                        && category.getContentType().equals(contentTypesByContentId.get(p.getTourismContentId())))
                .toList();
    }

    private MapSpotFeature toFeature(TourSpot spot) {

        MapGeometry geometry = MapGeometry.of(spot.mapX(), spot.mapY());
        TourSpotProperties properties = TourSpotProperties.from(spot);

        return MapSpotFeature.of(geometry, properties);
    }

    // 리포지토리 프로젝션 1건을 GeoJSON Feature(geometry+properties)로 변환
    private MapPostFeature toFeature(MapPostProjection projection, Map<String, Long> placePostCounts) {

        MapGeometry geometry = MapGeometry.of(projection.getLng(), projection.getLat());

        Integer placePostCount = projection.getTourismContentId() == null
                ? null
                : placePostCounts.get(projection.getTourismContentId()).intValue();

        MapPostProperties properties = MapPostProperties.from(projection, placePostCount);

        return MapPostFeature.of(geometry, properties);
    }

    /*
     * min < max, 위경도 범위(-90~90/-180~180) 검증
     * */
    private void validateBbox(double minLat, double minLng, double maxLat, double maxLng) {
        if (minLat >= maxLat || minLng >= maxLng) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (minLat < -90 || maxLat > 90 || minLng < -180 || maxLng > 180) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
