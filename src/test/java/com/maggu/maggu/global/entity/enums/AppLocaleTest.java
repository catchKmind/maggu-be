package com.maggu.maggu.global.entity.enums;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppLocaleTest {

    @Test
    @DisplayName("ko/kr/en 변형을 각각 KO/EN으로 파싱한다")
    void parsesCommonLanguageTags() {
        assertThat(AppLocale.from("ko")).isEqualTo(AppLocale.KO);
        assertThat(AppLocale.from("ko-KR")).isEqualTo(AppLocale.KO);
        assertThat(AppLocale.from("KR")).isEqualTo(AppLocale.KO);
        assertThat(AppLocale.from("en")).isEqualTo(AppLocale.EN);
        assertThat(AppLocale.from("en-US")).isEqualTo(AppLocale.EN);
    }

    @Test
    @DisplayName("알 수 없는 값은 INVALID_INPUT_VALUE다")
    void throwsForUnknownValue() {
        assertThatThrownBy(() -> AppLocale.from("jp"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("fromOrDefault는 null/blank를 KO로 떨어뜨린다")
    void fromOrDefaultFallsBackToKo() {
        assertThat(AppLocale.fromOrDefault(null)).isEqualTo(AppLocale.KO);
        assertThat(AppLocale.fromOrDefault("")).isEqualTo(AppLocale.KO);
        assertThat(AppLocale.fromOrDefault("en")).isEqualTo(AppLocale.EN);
    }
}
