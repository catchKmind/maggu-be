package com.maggu.maggu.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/*
 * GET /map/posts 응답 본문
 * GeoJSON FeatureCollection 형태로 bbox 안의 게시글 전체를 담는다.
 * */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapPostsResponse {

    @Schema(description = "GeoJSON 타입", example = "FeatureCollection")
    private String type = "FeatureCollection";

    @Schema(description = "bbox 내 게시글 목록")
    private List<MapPostFeature> features;

    private MapPostsResponse(List<MapPostFeature> features) {
        this.features = features;
    }

    public static MapPostsResponse of(List<MapPostFeature> features) {
        return new MapPostsResponse(features);
    }
}
