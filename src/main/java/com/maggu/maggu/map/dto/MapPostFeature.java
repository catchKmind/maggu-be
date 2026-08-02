package com.maggu.maggu.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * GeoJSON Feature 1개 = 게시글 1건 (좌표 geometry + 게시글 정보 properties)
 * */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapPostFeature {

    @Schema(description = "GeoJSON 타입", example = "Feature")
    private String type = "Feature";

    private MapPostGeometry geometry;

    private MapPostProperties properties;

    private MapPostFeature(MapPostGeometry geometry, MapPostProperties properties) {
        this.geometry = geometry;
        this.properties = properties;
    }

    public static MapPostFeature of(MapPostGeometry geometry, MapPostProperties properties) {
        return new MapPostFeature(geometry, properties);
    }
}
