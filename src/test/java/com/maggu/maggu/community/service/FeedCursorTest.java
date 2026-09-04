package com.maggu.maggu.community.service;

import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.post.entity.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedCursorTest {

    @Nested
    @DisplayName("encode/decode")
    class EncodeDecode {

        @Test
        @DisplayName("encode한 커서를 decode하면 원래 값으로 복원된다")
        void roundTrip() {
            FeedCursor cursor = new FeedCursor(12, Instant.ofEpochMilli(1_700_000_000_000L), 99L);

            FeedCursor decoded = FeedCursor.decode(cursor.encode());

            assertThat(decoded).isEqualTo(cursor);
        }

        @Test
        @DisplayName("Post로부터 만든 커서도 encode/decode 후 필드가 그대로 보존된다")
        void fromPostRoundTrip() {
            Post post = post(5L, 3, Instant.ofEpochMilli(1_700_000_000_000L));

            FeedCursor decoded = FeedCursor.decode(FeedCursor.from(post).encode());

            assertThat(decoded.scrapCount()).isEqualTo(3);
            assertThat(decoded.createdAt()).isEqualTo(Instant.ofEpochMilli(1_700_000_000_000L));
            assertThat(decoded.id()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Base64 형식이 아닌 문자열이면 잘못된 입력값 예외를 던진다")
        void throwsWhenNotBase64() {
            assertThatThrownBy(() -> FeedCursor.decode("!!!not-base64!!!"))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        }

        @Test
        @DisplayName("구분자 개수가 안 맞는 문자열이면 잘못된 입력값 예외를 던진다")
        void throwsWhenSegmentCountIsWrong() {
            String malformed = Base64.getEncoder().encodeToString("12:34".getBytes());

            assertThatThrownBy(() -> FeedCursor.decode(malformed))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        }

        @Test
        @DisplayName("숫자가 아닌 값이 섞여 있으면 잘못된 입력값 예외를 던진다")
        void throwsWhenSegmentIsNotNumeric() {
            String malformed = Base64.getEncoder().encodeToString("abc:34:1".getBytes());

            assertThatThrownBy(() -> FeedCursor.decode(malformed))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        }
    }

    private Post post(Long id, int scrapCount, Instant createdAt) {
        Post post = Post.builder().category(PostCategory.RECOMMEND).build();
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "scrapCount", scrapCount);
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        return post;
    }
}
