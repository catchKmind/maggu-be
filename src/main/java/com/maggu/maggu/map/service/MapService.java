package com.maggu.maggu.map.service;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.map.dto.MapPostFeature;
import com.maggu.maggu.map.dto.MapPostGeometry;
import com.maggu.maggu.map.dto.MapPostProperties;
import com.maggu.maggu.map.dto.MapPostsResponse;
import com.maggu.maggu.post.repository.MapPostProjection;
import com.maggu.maggu.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapService {

    private final PostRepository postRepository;

    // bbox 유효성 검증 후 그 안의 게시글을 조회해 GeoJSON FeatureCollection으로 변환해 반환
    public MapPostsResponse getMapPosts(double minLat, double minLng, double maxLat, double maxLng) {
        validateBbox(minLat, minLng, maxLat, maxLng);

        List<MapPostProjection> projections = postRepository.findInBbox(minLng, minLat, maxLng, maxLat);

        List<MapPostFeature> features = projections.stream()
                .map(this::toFeature)
                .toList();

        return MapPostsResponse.of(features);
    }

    // 리포지토리 프로젝션 1건을 GeoJSON Feature(geometry+properties)로 변환
    private MapPostFeature toFeature(MapPostProjection projection) {
        MapPostGeometry geometry = MapPostGeometry.of(projection.getLng(), projection.getLat());
        MapPostProperties properties = MapPostProperties.from(projection);
        return MapPostFeature.of(geometry, properties);
    }


    // min < max, 위경도 범위(-90~90/-180~180) 검증
    private void validateBbox(double minLat, double minLng, double maxLat, double maxLng) {
        if (minLat >= maxLat || minLng >= maxLng) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (minLat < -90 || maxLat > 90 || minLng < -180 || maxLng > 180) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}