package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * 원본 JSON 파싱용
 * /detailIntro2의 음식점(typeId: 39) item 1건
 */

@JsonIgnoreProperties(ignoreUnknown = true)
record DetailIntroRestaurantItem(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("contenttypeid")
        String contentTypeId,

        @JsonProperty("opentimefood")
        String openTime,

        @JsonProperty("restdatefood")
        String restDate,

        @JsonProperty("infocenterfood")
        String tel
) {
}
