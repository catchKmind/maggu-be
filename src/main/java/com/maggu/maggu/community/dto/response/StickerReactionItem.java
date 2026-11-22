package com.maggu.maggu.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record StickerReactionItem(
        @Schema(description = "스티커 ID")
        Long stickerId,

        @Schema(description = "스티커 이미지 URL")
        String imageUrl,

        @Schema(description = "이 스티커로 공감한 수")
        long count,

        @Schema(description = "현재 유저가 이 스티커로 공감했는지")
        boolean reactedByMe
) {
}
