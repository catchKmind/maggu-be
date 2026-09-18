package com.maggu.maggu.map.controller;

import com.maggu.maggu.map.dto.MapPostsResponse;
import com.maggu.maggu.map.dto.MapSpotDetail;
import com.maggu.maggu.map.dto.MapSpotsResponse;
import com.maggu.maggu.map.enums.MapCategory;
import com.maggu.maggu.map.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Map", description = "지도 관련 API")
@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @Operation(summary = "지도 범위 내 게시글 조회", description = "bbox(뷰포트) 내 게시글을 GeoJSON으로 반환한다. (줌 배율별 표시/클러스터링은 FE 담당)")
    @GetMapping("/posts")
    public MapPostsResponse getPosts(
            @Parameter(description = "bbox 최소 위도 (-90 ~ 90)") @RequestParam double minLat,
            @Parameter(description = "bbox 최소 경도 (-180 ~ 180)") @RequestParam double minLng,
            @Parameter(description = "bbox 최대 위도 (-90 ~ 90)") @RequestParam double maxLat,
            @Parameter(description = "bbox 최대 경도 (-180 ~ 180)") @RequestParam double maxLng,
            @Parameter(description = "카테고리 필터") @RequestParam(required = false) MapCategory category
    ) {
        return mapService.getMapPosts(minLat, minLng, maxLat, maxLng, category);
    }

    @Operation(summary = "지도 범위 내 관광지 스팟 조회",
            description = "bbox(뷰포트) 내 관광지 스팟 목록을 GeoJSON으로 반환한다. (캐시에 있는 스팟 기준, 캐시 미스 시 온디맨드 보충은 TODO)")
    @GetMapping("/spots")
    public MapSpotsResponse getSpots(
            @Parameter(description = "bbox 최소 위도 (-90 ~ 90)") @RequestParam double minLat,
            @Parameter(description = "bbox 최소 경도 (-180 ~ 180)") @RequestParam double minLng,
            @Parameter(description = "bbox 최대 위도 (-90 ~ 90)") @RequestParam double maxLat,
            @Parameter(description = "bbox 최대 경도 (-180 ~ 180)") @RequestParam double maxLng
    ) {
        return mapService.getMapSpots(minLat, minLng, maxLat, maxLng);
    }

    // 3459617 (39)
    @Operation(summary = "관광지 스팟 상세 조회",
            description = "contentId로 TourAPI(/detailCommon2, /detailImage2, /detailIntro2)를 실시간 호출해 " +
                    "이름/전화번호/주소/이미지/영업시간/휴무일/축제기간/좌표를 반환한다." +
                    "연결된 게시글 목록은 이 응답에 포함되지 않으며, GET /api/v1/community/posts/feed?contentId={contentId}로 별도 조회한다.")
    @GetMapping("/spots/{contentId}")
    public MapSpotDetail getSpot(@Parameter(description = "관광지 콘텐츠 ID") @PathVariable String contentId) {
        return mapService.getMapSpotDetail(contentId);
    }
}
