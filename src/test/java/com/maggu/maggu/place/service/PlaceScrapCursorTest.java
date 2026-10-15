package com.maggu.maggu.place.service;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceScrapCursorTest {

    @Test
    @DisplayName("encode한 커서를 decode하면 같은 createdAt/id로 복원된다")
    void roundTrips() {
        PlaceScrapCursor cursor = new PlaceScrapCursor(Instant.parse("2026-10-15T01:02:03.456Z"), 101L);

        PlaceScrapCursor decoded = PlaceScrapCursor.decode(cursor.encode());

        assertThat(decoded).isEqualTo(cursor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-base64!!", "", "MTIz"})
    @DisplayName("형식이 잘못된 커서면 INVALID_INPUT_VALUE 예외를 던진다")
    void throwsWhenMalformed(String cursor) {
        assertThatThrownBy(() -> PlaceScrapCursor.decode(cursor))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("숫자가 아닌 값이 들어 있으면 INVALID_INPUT_VALUE 예외를 던진다")
    void throwsWhenNotNumeric() {
        String cursor = Base64.getEncoder().encodeToString("abc:def".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> PlaceScrapCursor.decode(cursor))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }
}
