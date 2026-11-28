package com.maggu.maggu.map.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TourServiceArea {

    SEOUL(11, "서울특별시"),

    // TODO: DB 이전 후 활성화
//    JEONNAM_GWANGJU(12, "전남광주통합특별시"),
//
//    BUSAN(26, "부산광역시"),
//
//    DAEGU(27, "대구광역시"),
//
//    INCHEON(28, "인천광역시"),
//
//    DAEJEON(30, "대전광역시"),
//
//    ULSAN(31, "울산광역시"),

    GYEONGGI(41, "경기도"),

//    CHUNGBUK(43, "충청북도"),
//    CHUNGNAM(44, "충청남도"),

    GYEONGBUK(47, "경상북도"),
    GYEONGNAM(48, "경상남도"),

//    JEJU(50, "제주특별자치도"),
//
//    GANGWON(51, "강원특별자치도"),
//
//    JEONBUK(52, "전북특별자치도"),

    SEJONG(36110, "세종특별자치시");

    private final int lDongRegnCd;
    private final String koName;
}
