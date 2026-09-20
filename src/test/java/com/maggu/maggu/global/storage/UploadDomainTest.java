package com.maggu.maggu.global.storage;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadDomainTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("대소문자와 무관하게 정의된 도메인 이름이면 해당 enum 값을 반환한다")
        void returnsMatchingDomainRegardlessOfCase() {
            assertThat(UploadDomain.from("sticker")).isEqualTo(UploadDomain.STICKER);
            assertThat(UploadDomain.from("POST")).isEqualTo(UploadDomain.POST);
        }

        @Test
        @DisplayName("정의되지 않은 값이면 예외를 던진다")
        void throwsWhenValueNotDefined() {
            assertThatThrownBy(() -> UploadDomain.from("COMMENT"))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        }
    }
}
