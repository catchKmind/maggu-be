package com.maggu.maggu.sticker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StickerCreateRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "presigned-url 응답의 objectKey", example = "STICKER/8/a0d0809f-83cb-469e-9861-861fd3ff9ceb.png")
        String imageUrl
) {
}
