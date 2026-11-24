package com.maggu.maggu.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ProfileImageUpdateRequest(
        @NotBlank
        @Schema(description = "presigned-url 발급 시 받은 objectKey", example = "PROFILE/1/3f2a8c1e-9b7d-4e2a-8f1c-2d3e4f5a6b7c.png")
        String imageKey
) {
}
