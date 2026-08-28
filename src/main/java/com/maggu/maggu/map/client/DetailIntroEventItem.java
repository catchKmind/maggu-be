package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * 원본 JSON 파싱용
 * /detailIntro2의 행사/공연/축제(typeId: 15) item 1건
 */

@JsonIgnoreProperties(ignoreUnknown = true)
record DetailIntroEventItem(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("contenttypeid")
        String contentTypeId,

        @JsonProperty("eventstartdate")
        String eventStartDate,

        @JsonProperty("eventenddate")
        String eventEndDate,

        @JsonProperty("playtime")
        String playTime,

        @JsonProperty("sponsor1tel")
        String tel
) {
}
