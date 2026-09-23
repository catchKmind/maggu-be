package com.maggu.maggu.global.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;

public enum AppLocale {
    KO,
    EN;

    public static AppLocale defaultLocale() {
        return KO;
    }

    @JsonCreator
    public static AppLocale from(String value) {
        AppLocale locale = parse(value);
        if (locale == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return locale;
    }

    public static AppLocale fromOrDefault(String value) {
        AppLocale locale = parse(value);
        return locale == null ? KO : locale;
    }

    private static AppLocale parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_');
        if (normalized.startsWith("KO") || normalized.startsWith("KR")) {
            return KO;
        }
        if (normalized.startsWith("EN")) {
            return EN;
        }
        try {
            return AppLocale.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
