package com.maggu.maggu.sticker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StickerCreateRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "presigned URL 발급 응답의 objectKey. 예: STICKER/1/550e8400-e29b-41d4-a716-446655440000.png")
        String imageUrl
) {
}
