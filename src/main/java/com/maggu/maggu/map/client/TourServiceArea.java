package com.maggu.maggu.map.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TourServiceArea {

    GB(47, "경상북도"),
    GN(48, "경상남도");

    private final int lDongRegnCd;
    private final String name;
}
