package com.maggu.maggu.map.dto;

import com.maggu.maggu.map.client.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
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

        @Schema(description = "영업 시간")
        String businessHours,

        @Schema(description = "휴무일")
        String closedDays,

        @Schema(description = "축제 기간")
        String eventPeriod,

        @Schema(description = "경도 (tourAPI의 mapX 값)")
        Double lng,

        @Schema(description = "위도 (tourAPI의 mapY 값)")
        Double lat,

        // 이 장소(tourismContentId)에 걸린 게시글들의 scrap_count 합계
        // TODO: 장소 자체를 스크랩하는 기능이 생기면 그 스크랩 수로 교체될 예정
        @Schema(description = "이 장소에 걸린 게시글들의 스크랩수 합계")
        Integer placeScrapCount
) {
    // TourApiClient가 TourAPI 응답만으로 상세를 구성할 때 쓰는 생성자
    // placeScrapCount는 DB 조회가 필요해 TourApiClient가 알 수 없으므로, MapService가 이후 실제 값으로 다시 조립한다.
    public MapSpotDetail(String contentId, ContentType contentType, String tel, String title, String addr,
                          List<String> images, String businessHours, String closedDays, String eventPeriod,
                          Double lng, Double lat) {
        this(contentId, contentType, tel, title, addr, images, businessHours, closedDays, eventPeriod, lng, lat, null);
    }
}
