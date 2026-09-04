package com.maggu.maggu.global.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CursorPageResponseTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("size보다 1개 더 많이 오면(size+1) 초과분은 잘라내고 hasNext=true, nextCursor는 잘린 마지막 항목 기준이다")
        void returnsNextCursorWhenHasNext() {
            List<Long> fetched = List.of(9L, 8L, 7L); // size=2로 조회했다고 가정한 size+1개

            CursorPageResponse<Long> response = CursorPageResponse.of(fetched, 2, id -> "cursor-" + id);

            assertThat(response.getContent()).containsExactly(9L, 8L);
            assertThat(response.getNextCursor()).isEqualTo("cursor-8");
            assertThat(response.isHasNext()).isTrue();
        }

        @Test
        @DisplayName("size 이하로 오면 마지막 페이지이므로 hasNext=false, nextCursor는 null이다")
        void returnsNullCursorWhenNoNext() {
            List<Long> fetched = List.of(2L, 1L);

            CursorPageResponse<Long> response = CursorPageResponse.of(fetched, 3, id -> "cursor-" + id);

            assertThat(response.getContent()).containsExactly(2L, 1L);
            assertThat(response.getNextCursor()).isNull();
            assertThat(response.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("결과가 없으면 예외 없이 빈 목록과 null cursor를 반환한다")
        void returnsEmptyContentWhenNoResult() {
            CursorPageResponse<Long> response = CursorPageResponse.of(List.of(), 3, id -> "cursor-" + id);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getNextCursor()).isNull();
            assertThat(response.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("딱 size개만 오면(더 볼 것이 없는 경우) hasNext=false다")
        void returnsNoNextWhenFetchedExactlyMatchesSize() {
            List<Long> fetched = List.of(2L, 1L);

            CursorPageResponse<Long> response = CursorPageResponse.of(fetched, 2, id -> "cursor-" + id);

            assertThat(response.getContent()).containsExactly(2L, 1L);
            assertThat(response.isHasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("map")
    class Map {

        @Test
        @DisplayName("content 타입을 변환해도 nextCursor/hasNext는 그대로 유지된다")
        void preservesCursorMetadataAfterMap() {
            CursorPageResponse<Long> page = CursorPageResponse.of(List.of(9L, 8L, 7L), 2, id -> "cursor-" + id);

            CursorPageResponse<String> mapped = page.map(id -> "post-" + id);

            assertThat(mapped.getContent()).containsExactly("post-9", "post-8");
            assertThat(mapped.getNextCursor()).isEqualTo("cursor-8");
            assertThat(mapped.isHasNext()).isTrue();
        }
    }
}
