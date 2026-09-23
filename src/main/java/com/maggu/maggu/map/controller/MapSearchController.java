package com.maggu.maggu.map.controller;

import com.maggu.maggu.global.i18n.RequestLocaleResolver;
import com.maggu.maggu.map.dto.MapSpotDetail;
import com.maggu.maggu.post.dto.enums.FeedSort;
import com.maggu.maggu.post.dto.response.PostFeedItemResponse;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.map.dto.AutocompleteCandidateResponse;
import com.maggu.maggu.map.service.MapSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Map", description = "지도 관련 API")
@RestController
@RequestMapping("/api/v1/map/search")
@RequiredArgsConstructor
public class MapSearchController {

    private final MapSearchService mapSearchService;
    private final RequestLocaleResolver requestLocaleResolver;
    private static final int MAX_FEED_SIZE = 100;

    @Operation(summary = "지도 검색 자동완성 후보 조회",
            description = "검색창에 입력한 키워드로 관광지 자동완성 후보를 반환한다. " +
                    "TourSpotCache(관광지 캐시)의 title을 대상으로 매칭하며, 게시글이 없는 관광지도 후보에 포함될 수 있다. " +
                    "lang에 따라 KO/EN 캐시를 조회한다. 최대 6개까지 반환한다.")
    @GetMapping("/autocomplete")
    public List<AutocompleteCandidateResponse> getAutocompleteCandidates(
            @Parameter(description = "검색어(키워드)") @RequestParam String keyword,
            @Parameter(description = "TourAPI 언어. KO/EN. 생략 시 유저 locale 또는 KO") @RequestParam(required = false) String lang
    ) {
        return mapSearchService.getAutocompleteCandidates(keyword, requestLocaleResolver.resolve(lang));
    }

    @Operation(summary = "검색어 기반 게시글 피드 조회")
    @GetMapping("/posts")
    public CursorPageResponse<PostFeedItemResponse> getPosts(
            @Parameter(description = "검색어(키워드)") @RequestParam String keyword,
            @Parameter(description = "인기순(POPULAR) / 최신순(LATEST)으로 분류") @RequestParam String feedSort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        FeedSort sort = FeedSort.from(feedSort);
        int clampSize = Math.min(size, MAX_FEED_SIZE);

        return mapSearchService.searchPosts(keyword, sort, cursor, clampSize);
    }

    @Operation(summary = "검색어 기반 장소 조회",
            description = "검색어로 관광지 후보를 찾아(TourSpotCache title 매칭) " +
                    "각 장소를 GET /api/v1/map/spots/{contentId}와 동일한 형태로 조립해 리스트로 반환한다. " +
                    "후보마다 TourAPI를 실시간 호출하므로 최대 10개까지만 반환한다. lang=EN이면 EngService2를 호출한다.")
    @GetMapping("/spots")
    public List<MapSpotDetail> getSpots(
            @Parameter(description = "검색어(키워드)") @RequestParam String keyword,
            @Parameter(description = "TourAPI 언어. KO/EN. 생략 시 유저 locale 또는 KO") @RequestParam(required = false) String lang
    ) {
        return mapSearchService.searchSpots(keyword, requestLocaleResolver.resolve(lang));
    }
}
