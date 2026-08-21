package com.maggu.maggu.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/*
 * GET /map/posts 응답 본문
 * GeoJSON FeatureCollection 형태로 bbox 안의 게시글 전체를 담는다.
 */
public record MapPostsResponse(
        @Schema(description = "GeoJSON 타입", example = "FeatureCollection")
        String type,

        @Schema(description = "bbox 내 게시글 목록")
        List<MapPostFeature> features
) {
    public MapPostsResponse(List<MapPostFeature> features) {
        this("FeatureCollection", features);
    }

    public static MapPostsResponse of(List<MapPostFeature> features) {
        return new MapPostsResponse(features);
    }
}
