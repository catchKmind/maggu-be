package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * 원본 JSON 파싱용
 * /detailIntro2의 관광지(typeId: 12) item 1건
 */

@JsonIgnoreProperties(ignoreUnknown = true)
record DetailIntroAttractionItem(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("contenttypeid")
        String contentTypeId,

        @JsonProperty("restdate")
        String restDate,

        @JsonProperty("usetime")
        String useTime,

        @JsonProperty("infocenter")
        String infoCenter
) {
}
