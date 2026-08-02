package com.maggu.maggu.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/*
 * GeoJSON Feature의 geometry
 * 게시글 위치를 Point로 표현 (좌표 순서는 GeoJSON 표준대로 [경도, 위도])
 * */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapPostGeometry {

    @Schema(description = "GeoJSON 타입", example = "Point")
    private String type = "Point";

    @Schema(description = "[경도, 위도]", example = "[127.05, 37.55]")
    private List<Double> coordinates;

    private MapPostGeometry(List<Double> coordinates) {
        this.coordinates = coordinates;
    }

    public static MapPostGeometry of(double lng, double lat) {
        return new MapPostGeometry(List.of(lng, lat));
    }
}
