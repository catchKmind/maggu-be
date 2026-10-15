package com.maggu.maggu.place.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.global.i18n.RequestLocaleResolver;
import com.maggu.maggu.place.dto.request.PlaceScrapCreateRequest;
import com.maggu.maggu.place.dto.response.PlaceScrapCreateResponse;
import com.maggu.maggu.place.dto.response.PlaceFolderScrapsResponse;
import com.maggu.maggu.place.service.PlaceScrapService;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyPlace", description = "장소 스크랩 관련 API")
@RestController
@RequestMapping("/api/v1/my-places")
@RequiredArgsConstructor
public class PlaceScrapController {

    private static final int MAX_FEED_SIZE = 100;

    private final PlaceScrapService placeScrapService;
    private final RequestLocaleResolver requestLocaleResolver;

    @Operation(summary = "장소 스크랩 생성",
            description = "같은 폴더에 이미 저장된 장소면 실패한다.")
    @PostMapping("/scrap")
    public PlaceScrapCreateResponse createPlaceScrap(
            @CurrentUser AppUser user,
            @Valid @RequestBody PlaceScrapCreateRequest request
    ) {
        return placeScrapService.createPlaceScrap(user, request);
    }

    @Operation(summary = "폴더별 장소 스크랩 목록 조회",
            description = "본인 폴더의 장소 스크랩을 최신 저장순으로 반환한다. 다른 유저의 폴더면 403, 없는 폴더면 404")
    @GetMapping("/folders/{placeFolderId}/scraps")
    public PlaceFolderScrapsResponse getPlaceScraps(
            @CurrentUser AppUser user,
            @PathVariable Long placeFolderId,
            @Parameter(description = "TourAPI 언어. KO/EN. 생략 시 유저 locale 또는 KO") @RequestParam(required = false) String lang,
            @Parameter(description = "다음 페이지 커서. 첫 요청은 생략하고, 이후에는 직전 응답의 placeScraps.nextCursor를 그대로 넣는다. hasNext가 false면 더 요청하지 않는다")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기. 기본 20, 1~100 범위를 벗어나면 가까운 값으로 보정")
            @RequestParam(defaultValue = "20") int size
    ) {
        int clampSize = Math.max(1, Math.min(size, MAX_FEED_SIZE));

        return placeScrapService.getPlaceScraps(user, placeFolderId, requestLocaleResolver.resolve(lang), cursor, clampSize);
    }
}
