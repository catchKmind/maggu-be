package com.maggu.maggu.global.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CursorPageResponseTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("다음 페이지가 있으면 마지막 아이템의 id를 nextCursor로 반환한다")
        void returnsNextCursorWhenHasNext() {
            SliceImpl<Long> slice = new SliceImpl<>(List.of(9L, 8L, 7L), PageRequest.of(0, 3), true);

            CursorPageResponse<Long> response = CursorPageResponse.of(slice, id -> id);

            assertThat(response.getContent()).containsExactly(9L, 8L, 7L);
            assertThat(response.getNextCursor()).isEqualTo(7L);
            assertThat(response.isHasNext()).isTrue();
        }

        @Test
        @DisplayName("마지막 페이지면 nextCursor는 null이다")
        void returnsNullCursorWhenNoNext() {
            SliceImpl<Long> slice = new SliceImpl<>(List.of(2L, 1L), PageRequest.of(0, 3), false);

            CursorPageResponse<Long> response = CursorPageResponse.of(slice, id -> id);

            assertThat(response.getContent()).containsExactly(2L, 1L);
            assertThat(response.getNextCursor()).isNull();
            assertThat(response.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("결과가 없으면 예외 없이 빈 목록과 null cursor를 반환한다")
        void returnsEmptyContentWhenNoResult() {
            SliceImpl<Long> slice = new SliceImpl<>(List.of(), PageRequest.of(0, 3), false);

            CursorPageResponse<Long> response = CursorPageResponse.of(slice, id -> id);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getNextCursor()).isNull();
            assertThat(response.isHasNext()).isFalse();
        }
    }
}
