package com.maggu.maggu.mypage.dto;

import com.maggu.maggu.global.entity.enums.AppLocale;
import jakarta.validation.constraints.NotNull;

public record LocaleUpdateRequest(
        @NotNull AppLocale locale
) {
}
