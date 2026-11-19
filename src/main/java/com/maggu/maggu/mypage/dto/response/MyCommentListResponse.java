package com.maggu.maggu.mypage.dto.response;

import com.maggu.maggu.global.response.CursorPageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
public record MyCommentListResponse(
        @Schema(description = "내가 작성한 게시글 수 (페이지와 무관)")
        long postCount,

        @Schema(description = "내가 작성한 댓글 수 (페이지와 무관)")
        long commentCount,

        @Schema(description = "현재 페이지의 내 댓글 목록 (content/nextCursor/hasNext)")
        CursorPageResponse<MyCommentResponse> comments
) {

    @Builder
    public record MyCommentResponse(
            @Schema(description = "댓글 ID")
            Long commentId,

            @Schema(description = "게시글 ID")
            Long postId,

            @Schema(description = "원본 게시글 작성자 닉네임")
            String postWriterNickname,

            @Schema(description = "댓글 내용")
            String content,

            @Schema(description = "댓글 작성일")
            Instant createdAt,

            @Schema(description = "답글 여부")
            boolean isReply
    ) {

    }
}
