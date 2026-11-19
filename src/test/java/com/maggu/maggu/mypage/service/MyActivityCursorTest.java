package com.maggu.maggu.mypage.service;

import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.post.entity.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MyActivityCursorTest {

    @Test
    @DisplayName("encode한 커서를 decode하면 같은 createdAt/id로 복원된다")
    void roundTrips() {
        MyActivityCursor cursor = new MyActivityCursor(Instant.parse("2026-11-15T01:02:03.456Z"), 101L);

        MyActivityCursor decoded = MyActivityCursor.decode(cursor.encode());

        assertThat(decoded).isEqualTo(cursor);
    }

    @Test
    @DisplayName("게시글로 만든 커서는 게시글의 createdAt/id를 담는다")
    void createsFromPost() {
        Instant createdAt = Instant.parse("2026-11-15T00:00:00Z");
        Post post = Post.builder().content("내용").build();
        ReflectionTestUtils.setField(post, "id", 7L);
        ReflectionTestUtils.setField(post, "createdAt", createdAt);

        MyActivityCursor cursor = MyActivityCursor.from(post);

        assertThat(cursor).isEqualTo(new MyActivityCursor(createdAt, 7L));
    }

    @Test
    @DisplayName("댓글로 만든 커서는 댓글의 createdAt/id를 담는다")
    void createsFromComment() {
        Instant createdAt = Instant.parse("2026-11-15T00:00:00Z");
        Comment comment = Comment.builder().content("댓글").build();
        ReflectionTestUtils.setField(comment, "id", 9L);
        ReflectionTestUtils.setField(comment, "createdAt", createdAt);

        MyActivityCursor cursor = MyActivityCursor.from(comment);

        assertThat(cursor).isEqualTo(new MyActivityCursor(createdAt, 9L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-base64!!", "", "MTIz"})
    @DisplayName("형식이 잘못된 커서면 INVALID_INPUT_VALUE 예외를 던진다")
    void throwsWhenMalformed(String cursor) {
        assertThatThrownBy(() -> MyActivityCursor.decode(cursor))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Test
    @DisplayName("숫자가 아닌 값이 들어 있으면 INVALID_INPUT_VALUE 예외를 던진다")
    void throwsWhenNotNumeric() {
        String cursor = Base64.getEncoder().encodeToString("abc:def".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> MyActivityCursor.decode(cursor))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }
}
