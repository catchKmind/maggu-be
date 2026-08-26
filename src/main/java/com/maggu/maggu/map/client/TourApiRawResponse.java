package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record TourApiRawResponse<T>(Response<T> response) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response<T>(Header header, Body<T> body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Body<T>(
            @JsonDeserialize(using = TourApiItemsDeserializer.class)
            List<T> items
    ) {
    }
}
