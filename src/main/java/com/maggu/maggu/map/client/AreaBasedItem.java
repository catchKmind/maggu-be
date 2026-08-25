package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * 원본 JSON 파싱용
 * /areaBasedList2 응답 아이템 1건 파싱용
 * */
@JsonIgnoreProperties(ignoreUnknown = true)
record AreaBasedItem(
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
