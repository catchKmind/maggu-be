package com.maggu.maggu.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/*
 * GeoJSON Feature 1개 = 게시글 1건 (좌표 geometry + 게시글 정보 properties)
 */
public record MapPostFeature(
        @Schema(description = "GeoJSON 타입", example = "Feature")
        String type,

        MapGeometry geometry,

        MapPostProperties properties
) {
    public MapPostFeature(MapGeometry geometry, MapPostProperties properties) {
        this("Feature", geometry, properties);
    }

    public static MapPostFeature of(MapGeometry geometry, MapPostProperties properties) {
        return new MapPostFeature(geometry, properties);
    }
}
