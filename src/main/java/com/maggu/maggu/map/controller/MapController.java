package com.maggu.maggu.map.controller;

import com.maggu.maggu.map.dto.MapPostsResponse;
import com.maggu.maggu.map.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Map", description = "지도 관련 API")
@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @Operation(summary = "지도 범위 내 게시글 조회", description = "bbox(뷰포트) 내 게시글을 GeoJSON으로 반환한다. (줌 배율별 표시/클러스터링은 FE 담당)")
    @GetMapping("/posts")
    public MapPostsResponse getPosts(
            @Parameter(description = "bbox 최소 위도") @RequestParam double minLat,
            @Parameter(description = "bbox 최소 경도") @RequestParam double minLng,
            @Parameter(description = "bbox 최대 위도") @RequestParam double maxLat,
            @Parameter(description = "bbox 최대 경도") @RequestParam double maxLng
    ) {
        return mapService.getMapPosts(minLat, minLng, maxLat, maxLng);
    }
}
