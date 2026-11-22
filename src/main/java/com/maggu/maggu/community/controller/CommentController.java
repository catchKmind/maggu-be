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
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(summary = "댓글/대댓글 작성", description = "120자 제한. 대댓글 ID를 보내도 원댓글 아래로 귀속된다. 로그인 사용자만 작성 가능.")
    public CommentCreateResponse createComment(
            @CurrentUser AppUser user,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return commentService.createComment(user, postId, request);
    }

    @GetMapping("/comments")
    @Operation(summary = "댓글 목록 조회", description = "원댓글 아래 대댓글이 붙는 2단 구조. 닉네임은 가입 시 랜덤 닉네임, 글쓴이는 isPostWriter=true")
    public List<CommentResponse> getComments(
            @CurrentUser AppUser user,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId
    ) {
        return commentService.getComments(postId, user);
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "댓글 공감 토글", description = "이미 공감했으면 취소됨")
    public CommentLikeResponse toggleLike(
            @CurrentUser AppUser user,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId,
            @Parameter(description = "댓글 ID", example = "1") @PathVariable Long commentId
    ) {
        return commentService.toggleLike(user, commentId);
    }

    @PostMapping("/reactions")
    @Operation(summary = "스티커 반응", description = "같은 스티커 재클릭 시 취소, 다른 스티커면 교체")
    public StickerReactionResponse react(
            @CurrentUser AppUser user,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId,
            @Valid @RequestBody StickerReactionRequest request
    ) {
        return stickerReactionService.react(user, postId, request.getStickerId());
    }
}