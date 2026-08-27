package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record DetailImageItem(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("originimgurl")
        String originImgUrl,

        String cpyrhtDivCd
) {
}
