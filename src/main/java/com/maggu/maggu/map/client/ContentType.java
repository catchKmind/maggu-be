package com.maggu.maggu.map.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

// TourAPI contentTypeId와 매핑
@Getter
@RequiredArgsConstructor
public enum ContentType {

    TOURIST_ATTRACTION(12, 76, "관광지"),
    CULTURAL_FACILITY(14, 78, "문화시설"),
    FESTIVAL(15, 85, "축제공연행사"),
    TRAVEL_COURSE(25, null, "여행코스"), // 국문만 서비스
    LEISURE_SPORTS(28, 75, "레포츠"),
    ACCOMMODATION(32, 80, "숙박"),
    SHOPPING(38, 79, "쇼핑"),
    RESTAURANT(39, 82, "음식점"),
    TRAFFIC(null, 77, "교통"); // 다국어만 서비스

    @Getter(onMethod_ = @JsonValue)
    private final Integer koId;
    private final Integer multilingualId;
    private final String description;

    @JsonCreator // 역직렬화 시 숫자를 Enum으로 매핑
    public static ContentType fromId(int id) {
        return Arrays.stream(values())
                .filter(type -> Objects.equals(type.koId, id) || Objects.equals(type.multilingualId, id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown contentTypeId: " + id));
    }

    // detailIntro2 조회를 지원하는 타입인지 확인
    // TourApiClient.parseSpotDetail의 switch와 함께 수정해야 함
    public boolean supportsDetailIntro() {
        return this == TOURIST_ATTRACTION || this == FESTIVAL || this == RESTAURANT;
    }
}
