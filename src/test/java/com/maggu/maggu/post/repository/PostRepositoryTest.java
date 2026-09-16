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

    // findTopTourismContentIdsByScrapCount 전용 - 장소(tourism_content_id) 태깅과 scrapCount만 필요
    private Post persistPostWithTourismContentId(String tourismContentId, int scrapCount, boolean deleted) {
        Post post = Post.builder()
                .user(user)
                .slug("slug-" + slugSequence.incrementAndGet())
                .title("title")
                .content("content")
                .tourismContentId(tourismContentId)
                .category(PostCategory.RECOMMEND)
                .build();
        em.persist(post);
        ReflectionTestUtils.setField(post, "scrapCount", scrapCount);
        ReflectionTestUtils.setField(post, "deleted", deleted);
        em.flush();
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

    @Nested
    @DisplayName("findAllPopular")
    class FindAllPopular {

        @Test
        @DisplayName("키워드 매칭 없이 전체 게시글을 스크랩수 내림차순으로 반환한다")
        void ordersAllPostsByScrapCountDesc() {
            Post lowScrap = persistPost("게시글 A", null, 5, Instant.ofEpochMilli(3_000), false, true);
            Post highScrap = persistPost("게시글 B", null, 20, Instant.ofEpochMilli(1_000), false, true);
            em.clear();

            List<Post> result = postRepository.findAllPopular(null, null, null, 10);

            assertThat(result).extracting(Post::getId).containsExactly(highScrap.getId(), lowScrap.getId());
        }

        @Test
        @DisplayName("삭제된 게시글, 이미지 없는 게시글은 결과에서 제외한다")
        void excludesDeletedAndImagelessPosts() {
            persistPost("게시글 A", null, 10, Instant.ofEpochMilli(1_000), true, true);
            persistPost("게시글 B", null, 10, Instant.ofEpochMilli(1_000), false, false);
            em.clear();

            List<Post> result = postRepository.findAllPopular(null, null, null, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("커서(scrapCount, createdAt, id) 튜플보다 작은 게시글만 반환한다")
        void returnsOnlyPostsBeforeCursor() {
            Post before = persistPost("게시글 A", null, 20, Instant.ofEpochMilli(1_000), false, true);
            Post atCursor = persistPost("게시글 B", null, 30, Instant.ofEpochMilli(2_000), false, true);
            Post after = persistPost("게시글 C", null, 40, Instant.ofEpochMilli(3_000), false, true);
            em.clear();

            List<Post> result = postRepository.findAllPopular(
                    atCursor.getScrapCount(), atCursor.getCreatedAt(), atCursor.getId(), 10);

            assertThat(result).extracting(Post::getId).containsExactly(before.getId());
            assertThat(result).extracting(Post::getId).doesNotContain(atCursor.getId(), after.getId());
        }

        @Test
        @DisplayName("size만큼만 결과를 반환한다")
        void limitsResultBySize() {
            persistPost("게시글 A", null, 10, Instant.ofEpochMilli(1_000), false, true);
            persistPost("게시글 B", null, 20, Instant.ofEpochMilli(2_000), false, true);
            persistPost("게시글 C", null, 30, Instant.ofEpochMilli(3_000), false, true);
            em.clear();

            List<Post> result = postRepository.findAllPopular(null, null, null, 2);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findAllLatest")
    class FindAllLatest {

        @Test
        @DisplayName("키워드 매칭 없이 전체 게시글을 생성일시 내림차순으로 반환한다")
        void ordersAllPostsByCreatedAtDesc() {
            Post older = persistPost("게시글 A", null, 0, Instant.ofEpochMilli(1_000), false, true);
            Post newer = persistPost("게시글 B", null, 0, Instant.ofEpochMilli(2_000), false, true);
            em.clear();

            List<Post> result = postRepository.findAllLatest(null, null, 10);

            assertThat(result).extracting(Post::getId).containsExactly(newer.getId(), older.getId());
        }

        @Test
        @DisplayName("삭제된 게시글, 이미지 없는 게시글은 결과에서 제외한다")
        void excludesDeletedAndImagelessPosts() {
            persistPost("게시글 A", null, 0, Instant.ofEpochMilli(1_000), true, true);
            persistPost("게시글 B", null, 0, Instant.ofEpochMilli(1_000), false, false);
            em.clear();

            List<Post> result = postRepository.findAllLatest(null, null, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("커서(createdAt, id) 튜플보다 작은 게시글만 반환한다")
        void returnsOnlyPostsBeforeCursor() {
            Post before = persistPost("게시글 A", null, 0, Instant.ofEpochMilli(1_000), false, true);
            Post atCursor = persistPost("게시글 B", null, 0, Instant.ofEpochMilli(2_000), false, true);
            Post after = persistPost("게시글 C", null, 0, Instant.ofEpochMilli(3_000), false, true);
            em.clear();

            List<Post> result = postRepository.findAllLatest(atCursor.getCreatedAt(), atCursor.getId(), 10);

            assertThat(result).extracting(Post::getId).containsExactly(before.getId());
            assertThat(result).extracting(Post::getId).doesNotContain(atCursor.getId(), after.getId());
        }

        @Test
        @DisplayName("size만큼만 결과를 반환한다")
        void limitsResultBySize() {
            persistPost("게시글 A", null, 0, Instant.ofEpochMilli(1_000), false, true);
            persistPost("게시글 B", null, 0, Instant.ofEpochMilli(2_000), false, true);
            persistPost("게시글 C", null, 0, Instant.ofEpochMilli(3_000), false, true);
            em.clear();

            List<Post> result = postRepository.findAllLatest(null, null, 2);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findTopTourismContentIdsByScrapCount")
    class FindTopTourismContentIdsByScrapCount {

        @Test
        @DisplayName("장소(tourism_content_id)별 스크랩수 합계가 높은 순으로 contentId를 반환한다")
        void ordersByScrapCountSumDesc() {
            persistPostWithTourismContentId("100", 5, false);
            persistPostWithTourismContentId("100", 10, false); // 100 합계: 15
            persistPostWithTourismContentId("200", 30, false); // 200 합계: 30
            em.clear();

            List<String> result = postRepository.findTopTourismContentIdsByScrapCount(10);

            assertThat(result).containsExactly("200", "100");
        }

        @Test
        @DisplayName("tourism_content_id가 없는 게시글은 집계 대상에서 제외한다")
        void excludesPostsWithoutTourismContentId() {
            persistPost("장소 태깅 없음", null, 100, Instant.ofEpochMilli(1_000), false, true);
            em.clear();

            List<String> result = postRepository.findTopTourismContentIdsByScrapCount(10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("삭제된 게시글은 집계에서 제외한다")
        void excludesDeletedPosts() {
            persistPostWithTourismContentId("100", 100, true);
            em.clear();

            List<String> result = postRepository.findTopTourismContentIdsByScrapCount(10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("limit만큼만 결과를 반환한다")
        void limitsResultByLimit() {
            persistPostWithTourismContentId("100", 10, false);
            persistPostWithTourismContentId("200", 20, false);
            persistPostWithTourismContentId("300", 30, false);
            em.clear();

            List<String> result = postRepository.findTopTourismContentIdsByScrapCount(2);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("sumScrapCountByTourismContentId")
    class SumScrapCountByTourismContentId {

        @Test
        @DisplayName("같은 장소에 걸린 게시글들의 scrap_count 합계를 반환한다")
        void sumsScrapCountForSamePlace() {
            persistPostWithTourismContentId("100", 5, false);
            persistPostWithTourismContentId("100", 10, false);
            persistPostWithTourismContentId("200", 30, false); // 다른 장소, 합계에서 제외
            em.clear();

            int result = postRepository.sumScrapCountByTourismContentId("100");

            assertThat(result).isEqualTo(15);
        }

        @Test
        @DisplayName("삭제된 게시글은 합계에서 제외한다")
        void excludesDeletedPosts() {
            persistPostWithTourismContentId("100", 100, true);
            em.clear();

            int result = postRepository.sumScrapCountByTourismContentId("100");

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("걸린 게시글이 없으면 0을 반환한다")
        void returnsZeroWhenNoPostsTagThisPlace() {
            int result = postRepository.sumScrapCountByTourismContentId("존재하지않는contentId");

            assertThat(result).isZero();
        }
    }
}
