package com.maggu.maggu.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record PlaceFolderScrapsResponse(
        @Schema(description = "폴더 내의 스크랩된 장소 갯수")
        int totalScrapCount,

        @Schema(description = "폴더 정보")
        PlaceFolderResponse placeFolder,

        @Schema(description = "스크랩된 장소 정보 리스트")
        List<PlaceScrapResponse> placeScraps
) {
}
