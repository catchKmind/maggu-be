package com.maggu.maggu.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/*
 * GeoJSON Feature 1개 = spot 1건 (좌표 geometry + spot 정보 properties)
 */
public record MapSpotFeature(
        @Schema(description = "GeoJSON 타입", example = "Feature")
        String type,

        MapGeometry geometry,

        TourSpotProperties properties
) {
    public MapSpotFeature(MapGeometry geometry, TourSpotProperties properties) {
        this("Feature", geometry, properties);
    }

    public static MapSpotFeature of(MapGeometry geometry, TourSpotProperties properties) {
        return new MapSpotFeature(geometry, properties);
    }
}
