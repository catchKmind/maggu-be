package com.maggu.maggu.map.dto;

import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.client.TourSpot;
import io.swagger.v3.oas.annotations.media.Schema;

/*
 * TourApiClient가 반환하는 장소 스팟 1건 (도메인 값 객체)
 */
public record TourSpotProperties(
        @Schema(description = "contentID", example = "1357")
        String contentId,

        @Schema(description = "contentType", example = "39")
        ContentType contentType,

        @Schema(description = "장소명", example = "막꾸 음식점")
        String title,

        @Schema(description = "오늘 기준 진행중인 축제/공연/행사인지 여부. true면 지도 핀에 🔥(진행중 이벤트) 아이콘을 표시하는 데 사용한다. 축제가 아니거나 오늘 진행중이 아니면 false")
        boolean isOngoingEvent
) {
    public static TourSpotProperties from(TourSpot spot, boolean isOngoingEvent) {
        return new TourSpotProperties(spot.contentId(), spot.contentType(), spot.title(), isOngoingEvent);
    }
}
