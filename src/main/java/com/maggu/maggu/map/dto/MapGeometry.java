package com.maggu.maggu.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/*
 * GeoJSON Feature의 geometry
 * 좌표 순서는 GeoJSON 표준대로 [경도, 위도]
 */
public record MapGeometry(
        @Schema(description = "GeoJSON 타입", example = "Point")
        String type,

        @Schema(description = "[경도, 위도]", example = "[127.05, 37.55]")
        List<Double> coordinates
) {
    public MapGeometry(List<Double> coordinates) {
        this("Point", coordinates);
    }

    public static MapGeometry of(double lng, double lat) {
        return new MapGeometry(List.of(lng, lat));
    }
}
