package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * locationBasedList2의 item 1건. addr1/tel/firstimage 등 지금 안 쓰는 필드는 매핑하지 않는다.
 * mapx/mapy는 TourAPI가 JSON 문자열로 내려주므로 그대로 String으로 받는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TourApiItem(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("contenttypeid")
        String contentTypeId,

        @JsonProperty("mapx")
        String mapX,

        @JsonProperty("mapy")
        String mapY,

        String title
) {
}
