package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record DetailCommonItem(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("contenttypeid")
        String contentTypeId,

        String tel,

        String title,

        String addr1,

        String addr2,

        @JsonProperty("mapx")
        String mapX,

        @JsonProperty("mapy")
        String mapY
) {
}
