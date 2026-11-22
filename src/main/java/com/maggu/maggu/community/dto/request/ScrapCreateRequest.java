package com.maggu.maggu.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ScrapCreateRequest(
        @NotNull
        @Schema(description = "게시글 ID", example = "1")
        Long postId,

        @Schema(description = "폴더 ID. 없으면 기본 폴더", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long folderId
) {
}
