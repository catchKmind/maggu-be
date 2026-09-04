package com.maggu.maggu.community.dto.enums;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;

public enum FeedSort {
    POPULAR,
    LATEST;

    public static FeedSort from(String value) {
        try {
            return FeedSort.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
