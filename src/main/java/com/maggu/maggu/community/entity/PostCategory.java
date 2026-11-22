package com.maggu.maggu.community.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;

public enum PostCategory {
    CURATION,   // 큐레이션 - 키워드/주제별 묶음
    INFO,       // 정보 - 일시, 휴무, 개최 정보 등
    RECOMMEND,  // 추천
    QUESTION;   // 질문

    @JsonCreator
    public static PostCategory from(String value) {
        PostCategory category = parse(value);
        if (category == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return category;
    }

    private static PostCategory parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if (normalized.equals("QNA") || normalized.equals("QUESTION") || normalized.equals("질문")) {
            return QUESTION;
        }
        if (normalized.equals("RECOMMEND") || normalized.equals("추천")) {
            return RECOMMEND;
        }
        try {
            return PostCategory.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
