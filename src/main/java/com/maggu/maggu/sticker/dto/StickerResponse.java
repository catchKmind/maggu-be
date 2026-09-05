package com.maggu.maggu.sticker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record StickerResponse(
        @Schema(description = "스티커 ID")
        Long stickerId,

        @Schema(description = "스티커 이미지 url")
        String imageUrl
) {
}
