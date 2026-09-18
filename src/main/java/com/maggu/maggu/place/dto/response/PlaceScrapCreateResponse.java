package com.maggu.maggu.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PlaceScrapCreateResponse(
        @Schema(description = "id")
        Long placeScrapId,

        @Schema(description = "관광공사 콘텐츠 ID")
        String tourismContentId,

        @Schema(description = "스티커 ID")
        Long stickerId,

        @Schema(description = "장소 스크랩 폴더 ID")
        Long placeFolderId
) {
}
