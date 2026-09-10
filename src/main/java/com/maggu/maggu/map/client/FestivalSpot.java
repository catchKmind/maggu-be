package com.maggu.maggu.map.client;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record FestivalSpot(
        String contentId,

        ContentType contentType,

        String title,

        Double mapX,

        Double mapY,

        LocalDate eventStartDate,

        LocalDate eventEndDate
) {
}
