package com.maggu.maggu.global.storage;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;

public enum UploadDomain {
    STICKER,
    POST;

    public static UploadDomain from(String value) {
        try {
            return UploadDomain.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
