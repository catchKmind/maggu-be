package com.maggu.maggu.place.dto.response;

import com.maggu.maggu.global.response.CursorPageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PlaceFolderScrapsResponse(
        @Schema(description = "폴더 전체 장소 수(페이지와 무관)")
        long totalScrapCount,

        @Schema(description = "폴더 정보")
        PlaceFolderResponse placeFolder,

        @Schema(description = "현재 페이지의 장소 스크랩 목록(content/nextCursor/hasNext)")
        CursorPageResponse<PlaceScrapResponse> placeScraps
) {
}
