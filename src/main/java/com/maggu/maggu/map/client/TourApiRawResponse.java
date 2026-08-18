package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/*
 * TourAPI 원본 JSON 구조 그대로 매핑 (response.header / response.body.items)
 * numOfRows/pageNo/totalCount 등 지금 안 쓰는 필드는 ignoreUnknown으로 무시
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TourApiRawResponse(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Body(
            @JsonDeserialize(using = TourApiItemsDeserializer.class)
            List<TourApiItem> items
    ) {
    }
}
