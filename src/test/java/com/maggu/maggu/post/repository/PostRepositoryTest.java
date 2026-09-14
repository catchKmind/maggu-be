package com.maggu.maggu.post.repository;

import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.community.entity.PostImage;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PostRepository postRepository;

    private AtomicInteger slugSequence;
    private AppUser user;

    @BeforeEach
    void setUp() {
        slugSequence = new AtomicInteger();
        user = AppUser.builder()
                .provider(Provider.TEST)
                .providerUserId("provider-user-1")
                .email("tester@maggu.com")
                .nickname("tester")
                .build();
        em.persist(user);
    }

    // scrapCount/createdAt/deleted는 JPA 세터로 갱신 금지 대상이라, 테스트 데이터 세팅에서만 ReflectionTestUtils로 직접 값을 심는다(운영 코드 경로가 아님)
    private Post persistPost(String content, String placeName, int scrapCount, Instant createdAt, boolean deleted, boolean withImage) {
        Post post = Post.builder()
                .user(user)
                .slug("slug-" + slugSequence.incrementAndGet())
                .title("title")
                .content(content)
                .placeName(placeName)
                .category(PostCategory.RECOMMEND)
                .build();
        em.persist(post);
        ReflectionTestUtils.setField(post, "scrapCount", scrapCount);
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        ReflectionTestUtils.setField(post, "deleted", deleted);
        em.flush();

        if (withImage) {
            em.persist(PostImage.builder().post(post).imageUrl("https://img/" + post.getId() + ".jpg").sortOrder(0).build());
            em.flush();
        }
        return post;
    }

    @Nested
    @DisplayName("findByKeywordPopular")
    class FindByKeywordPopular {

        @Test
        @DisplayName("본문(content)에 키워드가 포함된 게시글을 반환한다")
        void matchesContentKeyword() {
            Post matched = persistPost("해운대 여행 후기", null, 0, Instant.ofEpochMilli(1_000), false, true);
            persistPost("광안리 여행 후기", null, 0, Instant.ofEpochMilli(1_000), false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordPopular("해운대", null, null, null, 10);

            assertThat(result).extracting(Post::getId).containsExactly(matched.getId());
        }

        @Test
        @DisplayName("장소명(place_name)에 키워드가 포함된 게시글을 반환한다")
        void matchesPlaceNameKeyword() {
            Post matched = persistPost("여행 다녀왔어요", "해운대해수욕장", 0, Instant.ofEpochMilli(1_000), false, true);
            persistPost("여행 다녀왔어요", "광안리해수욕장", 0, Instant.ofEpochMilli(1_000), false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordPopular("해운대", null, null, null, 10);

            assertThat(result).extracting(Post::getId).containsExactly(matched.getId());
        }

        @Test
        @DisplayName("삭제된 게시글은 결과에서 제외한다")
        void excludesDeletedPosts() {
            persistPost("해운대 여행", null, 0, Instant.ofEpochMilli(1_000), true, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordPopular("해운대", null, null, null, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("이미지가 없는 게시글은 결과에서 제외한다")
        void excludesPostsWithoutImage() {
            persistPost("해운대 여행", null, 0, Instant.ofEpochMilli(1_000), false, false);
            em.clear();

            List<Post> result = postRepository.findByKeywordPopular("해운대", null, null, null, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("스크랩수 내림차순으로 정렬하고, 스크랩수가 같으면 생성일시 내림차순으로 정렬한다")
        void ordersByScrapCountDescThenCreatedAtDesc() {
            Post lowScrap = persistPost("해운대 A", null, 5, Instant.ofEpochMilli(3_000), false, true);
            Post highScrap = persistPost("해운대 B", null, 20, Instant.ofEpochMilli(1_000), false, true);
            Post sameScrapNewer = persistPost("해운대 C", null, 20, Instant.ofEpochMilli(2_000), false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordPopular("해운대", null, null, null, 10);

            assertThat(result).extracting(Post::getId)
                    .containsExactly(sameScrapNewer.getId(), highScrap.getId(), lowScrap.getId());
        }

        @Test
        @DisplayName("커서(scrapCount, createdAt, id) 튜플보다 작은 게시글만 반환한다")
        void returnsOnlyPostsBeforeCursor() {
            Post before = persistPost("해운대 A", null, 20, Instant.ofEpochMilli(1_000), false, true);
            Post atCursor = persistPost("해운대 B", null, 30, Instant.ofEpochMilli(2_000), false, true);
            Post after = persistPost("해운대 C", null, 40, Instant.ofEpochMilli(3_000), false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordPopular(
                    "해운대", atCursor.getScrapCount(), atCursor.getCreatedAt(), atCursor.getId(), 10);

            assertThat(result).extracting(Post::getId).containsExactly(before.getId());
            assertThat(result).extracting(Post::getId).doesNotContain(atCursor.getId(), after.getId());
        }

        @Test
        @DisplayName("size만큼만 결과를 반환한다")
        void limitsResultBySize() {
            persistPost("해운대 A", null, 10, Instant.ofEpochMilli(1_000), false, true);
            persistPost("해운대 B", null, 20, Instant.ofEpochMilli(2_000), false, true);
            persistPost("해운대 C", null, 30, Instant.ofEpochMilli(3_000), false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordPopular("해운대", null, null, null, 2);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("키워드와 매칭되는 게시글이 없으면 빈 리스트를 반환한다")
        void returnsEmptyWhenNoMatch() {
            persistPost("광안리 여행", null, 0, Instant.ofEpochMilli(1_000), false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordPopular("해운대", null, null, null, 10);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByKeywordLatest")
    class FindByKeywordLatest {

        @Test
        @DisplayName("본문 또는 장소명에 키워드가 포함된 게시글을 반환한다")
        void matchesContentOrPlaceNameKeyword() {
            Post matchedByContent = persistPost("해운대 여행 후기", null, 0, Instant.ofEpochMilli(1_000), false, true);
            Post matchedByPlaceName = persistPost("여행 다녀왔어요", "해운대해수욕장", 0, Instant.ofEpochMilli(2_000), false, true);
            persistPost("광안리 여행 후기", "광안리해수욕장", 0, Instant.ofEpochMilli(3_000), false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordLatest("해운대", null, null, 10);

            assertThat(result).extracting(Post::getId)
                    .containsExactlyInAnyOrder(matchedByContent.getId(), matchedByPlaceName.getId());
        }

        @Test
        @DisplayName("삭제된 게시글은 결과에서 제외한다")
        void excludesDeletedPosts() {
            persistPost("해운대 여행", null, 0, Instant.ofEpochMilli(1_000), true, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordLatest("해운대", null, null, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("이미지가 없는 게시글은 결과에서 제외한다")
        void excludesPostsWithoutImage() {
            persistPost("해운대 여행", null, 0, Instant.ofEpochMilli(1_000), false, false);
            em.clear();

            List<Post> result = postRepository.findByKeywordLatest("해운대", null, null, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("생성일시 내림차순으로 정렬하고, 생성일시가 같으면 id 내림차순으로 정렬한다")
        void ordersByCreatedAtDescThenIdDesc() {
            Instant sameInstant = Instant.ofEpochMilli(2_000);
            Post oldest = persistPost("해운대 A", null, 0, Instant.ofEpochMilli(1_000), false, true);
            Post sameInstantFirst = persistPost("해운대 B", null, 0, sameInstant, false, true);
            Post sameInstantSecond = persistPost("해운대 C", null, 0, sameInstant, false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordLatest("해운대", null, null, 10);

            assertThat(result).extracting(Post::getId)
                    .containsExactly(sameInstantSecond.getId(), sameInstantFirst.getId(), oldest.getId());
        }

        @Test
        @DisplayName("커서(createdAt, id) 튜플보다 작은 게시글만 반환한다")
        void returnsOnlyPostsBeforeCursor() {
            Post before = persistPost("해운대 A", null, 0, Instant.ofEpochMilli(1_000), false, true);
            Post atCursor = persistPost("해운대 B", null, 0, Instant.ofEpochMilli(2_000), false, true);
            Post after = persistPost("해운대 C", null, 0, Instant.ofEpochMilli(3_000), false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordLatest(
                    "해운대", atCursor.getCreatedAt(), atCursor.getId(), 10);

            assertThat(result).extracting(Post::getId).containsExactly(before.getId());
            assertThat(result).extracting(Post::getId).doesNotContain(atCursor.getId(), after.getId());
        }

        @Test
        @DisplayName("size만큼만 결과를 반환한다")
        void limitsResultBySize() {
            persistPost("해운대 A", null, 0, Instant.ofEpochMilli(1_000), false, true);
            persistPost("해운대 B", null, 0, Instant.ofEpochMilli(2_000), false, true);
            persistPost("해운대 C", null, 0, Instant.ofEpochMilli(3_000), false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordLatest("해운대", null, null, 2);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("키워드와 매칭되는 게시글이 없으면 빈 리스트를 반환한다")
        void returnsEmptyWhenNoMatch() {
            persistPost("광안리 여행", null, 0, Instant.ofEpochMilli(1_000), false, true);
            em.clear();

            List<Post> result = postRepository.findByKeywordLatest("해운대", null, null, 10);

            assertThat(result).isEmpty();
        }
    }
}
