package com.maggu.maggu.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Schema(description = "새로운 닉네임 (30자 이내)")
        @NotBlank @Size(max = 30)
        String nickname,

        @Schema(description = "presigned-url 발급 시 받은 objectKey", example = "PROFILE/1/3f2a8c1e-9b7d-4e2a-8f1c-2d3e4f5a6b7c.png")
        String imageKey // null = 기본이미지
) {
    public ProfileUpdateRequest {
        nickname = nickname == null ? null : nickname.trim();
    }
}
