package com.maggu.maggu.community.controller;

import com.maggu.maggu.community.dto.request.FolderCreateRequest;
import com.maggu.maggu.community.dto.request.ScrapCreateRequest;
import com.maggu.maggu.community.dto.response.FolderCreateResponse;
import com.maggu.maggu.community.dto.response.FolderResponse;
import com.maggu.maggu.community.dto.response.PageResponse;
import com.maggu.maggu.community.dto.response.PostSummaryResponse;
import com.maggu.maggu.community.dto.response.ScrapResponse;
import com.maggu.maggu.community.service.ScrapService;
import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community")
public class ScrapController {

    private final ScrapService scrapService;

    @PostMapping("/folders")
    @Operation(summary = "스크랩 폴더 생성")
    public FolderCreateResponse createFolder(
            @CurrentUser AppUser user,
            @Valid @RequestBody FolderCreateRequest request
    ) {
        return scrapService.createFolder(user, request);
    }

    @GetMapping("/folders")
    @Operation(summary = "스크랩 폴더 목록 조회")
    public List<FolderResponse> getFolders(@CurrentUser AppUser user) {
        return scrapService.getFolders(user);
    }

    @PostMapping("/scraps")
    @Operation(summary = "게시글 스크랩", description = "folderId 없으면 기본 폴더에 저장")
    public ScrapResponse scrap(
            @CurrentUser AppUser user,
            @Valid @RequestBody ScrapCreateRequest request
    ) {
        return scrapService.scrap(user, request);
    }

    @DeleteMapping("/scraps/{postId}")
    @Operation(summary = "스크랩 취소")
    public ScrapResponse unscrap(
            @CurrentUser AppUser user,
            @PathVariable Long postId
    ) {
        return scrapService.unscrap(user, postId);
    }

    @PatchMapping("/scraps/{postId}/folder/{folderId}")
    @Operation(summary = "스크랩 폴더 이동")
    public ScrapResponse moveFolder(
            @CurrentUser AppUser user,
            @PathVariable Long postId,
            @PathVariable Long folderId
    ) {
        return scrapService.moveFolder(user, postId, folderId);
    }

    @GetMapping("/folders/{folderId}/scraps")
    @Operation(summary = "폴더별 스크랩 목록 조회")
    public PageResponse<PostSummaryResponse> getScraps(
            @CurrentUser AppUser user,
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return scrapService.getScrapsInFolder(user, folderId, page, size);
    }
}