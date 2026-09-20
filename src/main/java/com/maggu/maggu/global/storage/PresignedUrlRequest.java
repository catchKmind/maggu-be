package com.maggu.maggu.global.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresignedUrlRequest(
        @NotBlank
        @Size(max = 15)
        @Schema(description = "파일 타입 (예: image/png, image/jpeg, image/webp, image/heic)")
        String contentType,

        @NotBlank
        @Size(max = 15)
        @Schema(description = "도메인 (STICKER / POST)")
        String domain
) {
}
