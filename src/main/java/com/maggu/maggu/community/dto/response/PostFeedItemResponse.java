package com.maggu.maggu.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PostFeedItemResponse(
        @Schema(description = "게시글 ID")
        Long postId,

        @Schema(description = "대표 이미지 url")
        String imageUrl
) {
}
