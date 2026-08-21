package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * 원본 JSON 파싱용
 * locationBasedList2의 item 1건
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record LocationBasedItem(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("contenttypeid")
        String contentTypeId,

        String title,

        @JsonProperty("mapx")
        String mapX,

        @JsonProperty("mapy")
        String mapY
) {
}
