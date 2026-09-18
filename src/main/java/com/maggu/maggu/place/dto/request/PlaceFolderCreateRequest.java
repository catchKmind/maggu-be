package com.maggu.maggu.place.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceFolderCreateRequest(
        @NotBlank
        @Size(max = 50)
        @Schema(description = "장소 스크랩 폴더명")
        String name,

        @NotBlank
        @Size(max = 20)
        @Schema(description = "장소 스크랩 폴더 아이콘")
        String icon
) {
}
