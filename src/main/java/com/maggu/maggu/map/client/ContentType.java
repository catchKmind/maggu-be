package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

// TourAPI contentTypeId와 매핑
@Getter
@RequiredArgsConstructor
public enum ContentType {

    TOURIST_ATTRACTION(12, "관광지"),
    CULTURAL_FACILITY(14, "문화시설"),
    FESTIVAL(15, "축제공연행사"),
    TRAVEL_COURSE(25, "여행코스"),
    LEISURE_SPORTS(28, "레포츠"),
    ACCOMMODATION(32, "숙박"),
    SHOPPING(38, "쇼핑"),
    RESTAURANT(39, "음식점");

    @Getter(onMethod_ = @JsonValue)
    private final int id;
    private final String description;

    @JsonCreator // 역직렬화 시 숫자를 Enum으로 매핑
    public static ContentType fromId(int id) {
        return Arrays.stream(values())
                .filter(type -> type.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown contentTypeId: " + id));
    }
}
