package com.maggu.maggu.community.controller;

import com.maggu.maggu.community.dto.request.PostCreateRequest;
import com.maggu.maggu.community.dto.response.*;
import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.community.service.PostCommandService;
import com.maggu.maggu.community.service.PostQueryService;
import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/posts")
public class PostController {

    private final PostQueryService queryService;
    private final PostCommandService commandService;

    @PostMapping
    @Operation(summary = "게시글 작성", description = "사진 0~4장, 본문 500자, 사진이 있으면 위치 정보 필수")
    public PostCreateResponse createPost(
            @CurrentUser AppUser user,
            @Valid @RequestBody PostCreateRequest request
    ) {
        return commandService.createPost(user, request);
    }

    @GetMapping
    @Operation(summary = "커뮤니티 피드 조회", description = "카테고리 탭별, 최신순(latest)/스크랩 많은순(popular) 조회")
    public PageResponse<PostSummaryResponse> getFeed(
            @CurrentUser AppUser user,
            @RequestParam(required = false) PostCategory category,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return queryService.getFeed(category, sort, user, page, size);
    }

    @GetMapping("/search")
    @Operation(summary = "게시글 검색", description = "본문/장소명 키워드 검색")
    public PageResponse<PostSummaryResponse> search(
            @CurrentUser AppUser user,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return queryService.search(keyword, sort, user, page, size);
    }

    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회")
    public PostDetailResponse getDetail(
            @CurrentUser AppUser user,
            @PathVariable Long postId
    ) {
        return queryService.getDetail(postId, user);
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제", description = "작성자 본인만 삭제 가능")
    public PostDeleteResponse deletePost(
            @CurrentUser AppUser user,
            @PathVariable Long postId
    ) {
        return commandService.deletePost(user, postId);
    }

    @GetMapping("/{postId}/share")
    @Operation(summary = "게시글 공유 링크 조회")
    public PostShareResponse share(@PathVariable Long postId) {
        return queryService.getShareLink(postId);
    }

    @GetMapping("/search/autocomplete")
    @Operation(summary = "연관 검색어 조회 (자동완성)", description = "검색어 입력 시 장소명 기반 연관 검색어 최대 6개 제공")
    public SearchAutocompleteResponse getAutocomplete(
            @RequestParam String keyword
    ) {
        return queryService.getAutocomplete(keyword);
    }

    @GetMapping("/curation")
    @Operation(summary = "큐레이션 탭 조회", description = "주제/키워드별 상위 5개 게시글 좌우 슬라이드 카드 목록 반환")
    public List<CurationResponse> getCuration(
            @CurrentUser AppUser user
    ) {
        return queryService.getCuration(user);
    }
}