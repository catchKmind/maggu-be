package com.maggu.maggu.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PlaceScrapResponse(
        @Schema(description = "장소 스크랩 ID")
        Long placeScrapId,

        @Schema(description = "관광공사 콘텐츠 ID", example = "13579")
        String tourismContentId,

        @Schema(description = "장소명, 캐시에 없는 장소면 null, 다른 언어로 저장한 장소도 null")
        String placeName,

        @Schema(description = "주소, 현재는 항상 null, 후속 PR에서-!")
        String placeAddress,

        @Schema(description = "스티커 ID")
        Long stickerId
) {
}
