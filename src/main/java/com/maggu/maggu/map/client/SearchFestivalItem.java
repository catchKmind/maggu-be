package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * 원본 JSON 파싱용
 * /searchFestival2 응답 아이템 1건 파싱용
 * */
record SearchFestivalItem(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("contenttypeid")
        String contentTypeId,

        String title,

        @JsonProperty("eventstartdate")
        String eventStartDate,

        @JsonProperty("eventenddate")
        String eventEndDate,

        @JsonProperty("mapx")
        String mapX,

        @JsonProperty("mapy")
        String mapY
) {
}
