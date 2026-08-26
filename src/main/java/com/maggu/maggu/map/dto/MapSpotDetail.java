package com.maggu.maggu.map.dto;

import com.maggu.maggu.map.client.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MapSpotDetail(
        @Schema(description = "콘텐츠 ID", example = "13579")
        String contentId,

        @Schema(description = "관광타입 ID",
                example = "12: 관광지, 14: 문화시설, 15: 축제공연행사, 25: 여행코스, 28: 레포츠, 32: 숙박, 38: 쇼핑, 39: 음식점")
        ContentType contentType,

        @Schema(description = "전화번호")
        String tel,

        @Schema(description = "콘텐츠명")
        String title,

        @Schema(description = "주소")
        String addr,

        @Schema(description = "대표 이미지 url List")
        List<String> images,

        @Schema(description = "경도 (tourAPI의 mapX 값)")
        Double lng,

        @Schema(description = "위도 (tourAPI의 mapY 값)")
        Double lat

        // TODO: post 관련 추가
) {
}
