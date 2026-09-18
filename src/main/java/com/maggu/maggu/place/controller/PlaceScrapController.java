package com.maggu.maggu.place.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.place.dto.request.PlaceScrapCreateRequest;
import com.maggu.maggu.place.dto.response.PlaceScrapCreateResponse;
import com.maggu.maggu.place.service.PlaceScrapService;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyPlace", description = "장소 스크랩 관련 API")
@RestController
@RequestMapping("/api/v1/my-places")
@RequiredArgsConstructor
public class PlaceScrapController {

    private final PlaceScrapService placeScrapService;

    @Operation(summary = "장소 스크랩 생성",
            description = "같은 폴더에 이미 저장된 장소면 실패한다.")
    @PostMapping("/scrap")
    public PlaceScrapCreateResponse createPlaceScrap(
            @CurrentUser AppUser user,
            @Valid @RequestBody PlaceScrapCreateRequest request
    ) {
        return placeScrapService.createPlaceScrap(user, request);
    }
}
