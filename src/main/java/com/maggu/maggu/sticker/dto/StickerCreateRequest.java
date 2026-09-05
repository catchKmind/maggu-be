package com.maggu.maggu.sticker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StickerCreateRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "스티커 이미지 URL")
        String imageUrl
) {
}
