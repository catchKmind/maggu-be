package com.maggu.maggu.community.repository;

import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.user.entity.AppUser;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CommentRepository commentRepository;

    private AtomicInteger slugSequence;
    private AppUser me;
    private AppUser postWriter;
    private Post post;

    @BeforeEach
    void setUp() {
        slugSequence = new AtomicInteger();
        me = persistUser("me");
        postWriter = persistUser("writer");
        post = persistPost(postWriter, false);
    }

    private AppUser persistUser(String nickname) {
        AppUser user = AppUser.builder()
                .provider(Provider.TEST)
                .providerUserId("provider-" + nickname)
                .email(nickname + "@maggu.com")
                .nickname(nickname)
                .build();
        em.persist(user);
        return user;
    }

    // deleted/createdAt은 운영 코드에서 세터로 바꾸지 않는 필드라, 테스트 데이터 세팅에서만 ReflectionTestUtils로 값을 심는다
    private Post persistPost(AppUser writer, boolean deleted) {
        Post newPost = Post.builder()
                .user(writer)
                .slug("slug-" + slugSequence.incrementAndGet())
                .title("title")
                .content("content")
                .category(PostCategory.RECOMMEND)
                .build();
        em.persist(newPost);
        ReflectionTestUtils.setField(newPost, "deleted", deleted);
        em.flush();
        return newPost;
    }

    private Comment persistComment(Post targetPost, AppUser writer, Instant createdAt, boolean deleted) {
        Comment comment = Comment.builder()
                .post(targetPost)
                .user(writer)
                .content("comment")
                .build();
        em.persist(comment);
        ReflectionTestUtils.setField(comment, "createdAt", createdAt);
        ReflectionTestUtils.setField(comment, "deleted", deleted);
        em.flush();
        return comment;
    }

    @Nested
    @DisplayName("findPageByUserId")
    class FindPageByUserId {

        @Test
        @DisplayName("커서가 없으면 작성일 최신순으로 limit개까지 반환한다")
        void returnsFirstPageOrderedByCreatedAtDesc() {
            Comment oldest = persistComment(post, me, Instant.parse("2026-11-01T00:00:00Z"), false);
            Comment newest = persistComment(post, me, Instant.parse("2026-11-03T00:00:00Z"), false);
            Comment middle = persistComment(post, me, Instant.parse("2026-11-02T00:00:00Z"), false);
            em.clear();

            List<Comment> result = commentRepository.findPageByUserId(me.getId(), null, null, Limit.of(2));

            assertThat(result).extracting(Comment::getId).containsExactly(newest.getId(), middle.getId());
        }

        @Test
        @DisplayName("커서가 있으면 커서보다 오래된 댓글만 이어서 반환한다")
        void returnsCommentsOlderThanCursor() {
            Comment oldest = persistComment(post, me, Instant.parse("2026-11-01T00:00:00Z"), false);
            Comment middle = persistComment(post, me, Instant.parse("2026-11-02T00:00:00Z"), false);
            persistComment(post, me, Instant.parse("2026-11-03T00:00:00Z"), false);
            em.clear();

            List<Comment> result = commentRepository.findPageByUserId(
                    me.getId(), middle.getCreatedAt(), middle.getId(), Limit.of(10));

            assertThat(result).extracting(Comment::getId).containsExactly(oldest.getId());
        }

        @Test
        @DisplayName("작성일이 같으면 id 내림차순으로 정렬하고, 커서도 id로 구분해 중복/누락이 없다")
        void breaksTiesById() {
            Instant sameTime = Instant.parse("2026-11-02T00:00:00Z");
            Comment first = persistComment(post, me, sameTime, false);
            Comment second = persistComment(post, me, sameTime, false);
            Comment third = persistComment(post, me, sameTime, false);
            em.clear();

            List<Comment> firstPage = commentRepository.findPageByUserId(me.getId(), null, null, Limit.of(2));
            Comment lastOfFirstPage = firstPage.get(firstPage.size() - 1);
            List<Comment> secondPage = commentRepository.findPageByUserId(
                    me.getId(), lastOfFirstPage.getCreatedAt(), lastOfFirstPage.getId(), Limit.of(2));

            assertThat(firstPage).extracting(Comment::getId).containsExactly(third.getId(), second.getId());
            assertThat(secondPage).extracting(Comment::getId).containsExactly(first.getId());
        }

        @Test
        @DisplayName("삭제된 댓글과 삭제된 게시글에 단 댓글은 제외한다")
        void excludesDeletedCommentsAndCommentsOnDeletedPosts() {
            Post deletedPost = persistPost(postWriter, true);
            Comment visible = persistComment(post, me, Instant.parse("2026-11-01T00:00:00Z"), false);
            persistComment(post, me, Instant.parse("2026-11-02T00:00:00Z"), true);
            persistComment(deletedPost, me, Instant.parse("2026-11-03T00:00:00Z"), false);
            em.clear();

            List<Comment> result = commentRepository.findPageByUserId(me.getId(), null, null, Limit.of(10));

            assertThat(result).extracting(Comment::getId).containsExactly(visible.getId());
        }

        @Test
        @DisplayName("다른 유저가 쓴 댓글은 섞이지 않는다")
        void doesNotIncludeOtherUsersComments() {
            Comment mine = persistComment(post, me, Instant.parse("2026-11-01T00:00:00Z"), false);
            persistComment(post, postWriter, Instant.parse("2026-11-02T00:00:00Z"), false);
            em.clear();

            List<Comment> result = commentRepository.findPageByUserId(me.getId(), null, null, Limit.of(10));

            assertThat(result).extracting(Comment::getId).containsExactly(mine.getId());
        }

        @Test
        @DisplayName("원본 게시글과 게시글 작성자를 함께 로딩한다(fetch join)")
        void fetchesPostAndPostWriter() {
            persistComment(post, me, Instant.parse("2026-11-01T00:00:00Z"), false);
            em.clear();

            List<Comment> result = commentRepository.findPageByUserId(me.getId(), null, null, Limit.of(10));

            Comment comment = result.get(0);
            assertThat(Hibernate.isInitialized(comment.getPost())).isTrue();
            assertThat(Hibernate.isInitialized(comment.getPost().getUser())).isTrue();
            assertThat(comment.getPost().getUser().getNickname()).isEqualTo("writer");
        }

        @Test
        @DisplayName("작성한 댓글이 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoComments() {
            List<Comment> result = commentRepository.findPageByUserId(me.getId(), null, null, Limit.of(10));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByUserIdAndDeletedFalseAndPostDeletedFalse")
    class CountByUserIdAndDeletedFalseAndPostDeletedFalse {

        @Test
        @DisplayName("삭제되지 않은 게시글에 단, 삭제되지 않은 내 댓글만 센다")
        void countsOnlyVisibleCommentsOfUser() {
            Post deletedPost = persistPost(postWriter, true);
            persistComment(post, me, Instant.parse("2026-11-01T00:00:00Z"), false);
            persistComment(post, me, Instant.parse("2026-11-02T00:00:00Z"), false);
            persistComment(post, me, Instant.parse("2026-11-03T00:00:00Z"), true);
            persistComment(deletedPost, me, Instant.parse("2026-11-04T00:00:00Z"), false);
            persistComment(post, postWriter, Instant.parse("2026-11-05T00:00:00Z"), false);
            em.clear();

            long result = commentRepository.countByUserIdAndDeletedFalseAndPostDeletedFalse(me.getId());

            assertThat(result).isEqualTo(2);
        }
    }
}
