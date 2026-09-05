package com.maggu.maggu.sticker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record StickerDeleteResponse(
        @Schema(description = "스티커 ID")
        Long stickerId,

        @Schema(description = "삭제 여부")
        boolean deleted
) {
}
