package com.maggu.maggu.community.controller;

import com.maggu.maggu.community.dto.request.CommentCreateRequest;
import com.maggu.maggu.community.dto.request.StickerReactionRequest;
import com.maggu.maggu.community.dto.response.CommentCreateResponse;
import com.maggu.maggu.community.dto.response.CommentLikeResponse;
import com.maggu.maggu.community.dto.response.CommentResponse;
import com.maggu.maggu.community.dto.response.StickerReactionResponse;
import com.maggu.maggu.community.service.CommentService;
import com.maggu.maggu.community.service.StickerReactionService;
import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/posts/{postId}")
public class CommentController {

    private final CommentService commentService;
    private final StickerReactionService stickerReactionService;

    @PostMapping("/comments")
    @Operation(summary = "댓글/대댓글 작성", description = "120자 제한, 대댓글은 1단계까지만 허용")
    public CommentCreateResponse createComment(
            @CurrentUser AppUser user,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return commentService.createComment(user, postId, request);
    }

    @GetMapping("/comments")
    @Operation(summary = "댓글 목록 조회", description = "최상위 댓글 + 대댓글 트리 구조로 반환")
    public List<CommentResponse> getComments(
            @CurrentUser AppUser user,
            @PathVariable Long postId
    ) {
        return commentService.getComments(postId, user);
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "댓글 공감 토글", description = "이미 공감했으면 취소됨")
    public CommentLikeResponse toggleLike(
            @CurrentUser AppUser user,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        return commentService.toggleLike(user, commentId);
    }

    @PostMapping("/reactions")
    @Operation(summary = "스티커 반응", description = "같은 스티커 재클릭 시 취소, 다른 스티커면 교체")
    public StickerReactionResponse react(
            @CurrentUser AppUser user,
            @PathVariable Long postId,
            @Valid @RequestBody StickerReactionRequest request
    ) {
        return stickerReactionService.react(user, postId, request.getStickerId());
    }
}