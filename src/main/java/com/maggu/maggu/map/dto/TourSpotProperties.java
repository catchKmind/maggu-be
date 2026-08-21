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
        String title
) {
    public static TourSpotProperties from(TourSpot spot) {
        return new TourSpotProperties(spot.contentId(), spot.contentType(), spot.title());
    }
}
