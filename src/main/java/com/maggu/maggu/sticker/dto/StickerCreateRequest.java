package com.maggu.maggu.sticker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StickerCreateRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "presigned-url 응답의 objectKey", example = "STICKER/12/uuid.png")
        String imageUrl
) {
}
