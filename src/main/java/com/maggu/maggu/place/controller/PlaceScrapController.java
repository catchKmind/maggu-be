package com.maggu.maggu.place.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.place.dto.request.PlaceFolderCreateRequest;
import com.maggu.maggu.place.dto.response.PlaceFolderCreateResponse;
import com.maggu.maggu.place.dto.response.PlaceFolderResponse;
import com.maggu.maggu.place.service.PlaceScrapService;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "MyPlace", description = "장소 스크랩 관련 API")
@RestController
@RequestMapping("/api/v1/my-places")
@RequiredArgsConstructor
public class PlaceScrapController {

    private final PlaceScrapService placeScrapService;

    @Operation(summary = "장소 스크랩 폴더 목록 조회",
            description = "기본 폴더(내 장소)가 항상 먼저 오도록 정렬해서 반환한다.")
    @GetMapping("/folders")
    public List<PlaceFolderResponse> getPlaceFolders(
            @CurrentUser AppUser user
    ) {
        return placeScrapService.getPlaceFolders(user);
    }

    @Operation(summary = "장소 스크랩 폴더 생성",
            description = "같은 이름의 폴더가 있으면 생성에 실패한다.")
    @PostMapping("/folders")
    public PlaceFolderCreateResponse createPlaceFolder(
            @CurrentUser AppUser user,
            @Valid @RequestBody PlaceFolderCreateRequest request
    ) {
        return placeScrapService.createPlaceFolder(user, request);
    }
}
