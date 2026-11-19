package com.maggu.maggu.mypage.dto.response;

import com.maggu.maggu.global.response.CursorPageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MyPostListResponse(
        @Schema(description = "내가 작성한 게시글 수 (페이지와 무관)")
        long postCount,

        @Schema(description = "내가 작성한 댓글 수 (페이지와 무관)")
        long commentCount,

        @Schema(description = "현재 페이지의 내 게시글 목록 (content/nextCursor/hasNext)")
        CursorPageResponse<MyPostResponse> posts
) {
    @Builder
    public record MyPostResponse(
            @Schema(description = "게시글 ID")
            Long postId,

            @Schema(description = "대표 이미지 URL, 사진 없는 글이면 null")
            String imageUrl,

            @Schema(description = "게시글 본문 전체. 사진 없는 글은 FE가 잘라 표시")
            String content
    ) {
    }
}
