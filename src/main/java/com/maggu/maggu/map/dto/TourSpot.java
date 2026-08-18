package com.maggu.maggu.map.dto;

import com.maggu.maggu.map.client.ContentType;

/*
 * TourApiClient가 반환하는 관광지 후보 1건 (도메인 값 객체)
 */
public record TourSpot(
        String contentId,

        ContentType contentType,

        String title,

        Double mapX,

        Double mapY
) {
}
