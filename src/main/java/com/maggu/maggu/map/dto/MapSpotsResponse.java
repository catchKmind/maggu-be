package com.maggu.maggu.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/*
 * GET /map/spots 응답 본문
 * GeoJSON FeatureCollection 형태로 bbox 안의 스팟 전체를 담는다.
 */
public record MapSpotsResponse(
        @Schema(description = "GeoJSON 타입", example = "FeatureCollection")
        String type,

        @Schema(description = "bbox 내 스팟 목록")
        List<MapSpotFeature> features
) {
    public MapSpotsResponse(List<MapSpotFeature> features) {
        this("FeatureCollection", features);
    }

    public static MapSpotsResponse of(List<MapSpotFeature> features) {
        return new MapSpotsResponse(features);
    }
}
