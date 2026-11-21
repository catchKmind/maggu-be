package com.maggu.maggu.user.dto.request;

import com.maggu.maggu.global.entity.enums.AppLocale;
import jakarta.validation.constraints.NotNull;

public record LocaleUpdateRequest(
        @NotNull AppLocale locale
) {
}
