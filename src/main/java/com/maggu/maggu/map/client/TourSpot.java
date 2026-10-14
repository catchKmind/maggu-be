package com.maggu.maggu.map.client;

import lombok.Builder;

@Builder
public record TourSpot(
        String contentId,

        ContentType contentType,

        String title,

        String address,

        Double mapX,

        Double mapY
) {
}
