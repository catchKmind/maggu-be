package com.maggu.maggu.post.dto.enums;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;

public enum FeedSort {
    MOST_SCRAPPED,
    LATEST;

    public static FeedSort from(String value) {
        FeedSort sort = parse(value);
        if (sort == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return sort;
    }

    public static FeedSort fromOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return LATEST;
        }
        return from(value);
    }

    private static FeedSort parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_');
        if (normalized.equals("LATEST")) {
            return LATEST;
        }
        if (normalized.equals("MOST_SCRAPPED")) {
            return MOST_SCRAPPED;
        }
        return null;
    }
}
