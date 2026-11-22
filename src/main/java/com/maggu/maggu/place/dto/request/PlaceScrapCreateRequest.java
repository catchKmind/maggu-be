package com.maggu.maggu.place.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceScrapCreateRequest(
        @NotBlank
        @Schema(description = "관광공사 콘텐츠 ID", example = "126081")
        String tourismContentId,

        @NotNull
        @Schema(description = "스티커 ID", example = "1")
        Long stickerId,

        @NotNull
        @Schema(description = "장소 스크랩 폴더 ID", example = "1")
        Long placeFolderId
) {
}
