package com.maggu.maggu.map.enums;

import com.maggu.maggu.map.client.ContentType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
 * /map/posts의 category 필터
 * FOOD/LANDMARK/STAY는 TourAPI ContentType과 매핑되는 장소 타입 필터
 * 카페/맛집은 TourAPI상 세부코드가 같아 FOOD 하나로 통합
 * POPULAR(Hot Places)는 TourAPI 호출 없이 scrap_count로 정렬하는 필터라 contentType 없음(null)
 */
@Getter
@RequiredArgsConstructor
public enum MapCategory {

    FOOD(ContentType.RESTAURANT),
    LANDMARK(ContentType.TOURIST_ATTRACTION),
    STAY(ContentType.ACCOMMODATION),
    POPULAR(null);

    private final ContentType contentType;
}
