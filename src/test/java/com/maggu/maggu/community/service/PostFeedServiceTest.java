package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.enums.FeedSort;
import com.maggu.maggu.community.dto.response.PostFeedItemResponse;
import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.community.entity.PostImage;
import com.maggu.maggu.community.repository.PostImageRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PostFeedServiceTest {

    private static final String CONTENT_ID = "126234";

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @InjectMocks
    private PostFeedService postFeedService;

    @Nested
    @DisplayName("getFeed")
    class GetFeed {

        @Test
        @DisplayName("POPULAR 정렬이면 인기순 조회 메서드를 호출하고, 최신순 조회 메서드는 호출하지 않는다")
        void popularSortCallsPopularRepositoryMethod() {
            given(postRepository.findPostsByContentIdPopular(CONTENT_ID, null, null, null, 3))
                    .willReturn(List.of());
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of())).willReturn(List.of());

            postFeedService.getFeed(CONTENT_ID, FeedSort.POPULAR, null, 2);

            verify(postRepository).findPostsByContentIdPopular(CONTENT_ID, null, null, null, 3);
            verify(postRepository, never()).findPostsByContentIdLatest(any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("LATEST 정렬이면 최신순 조회 메서드를 호출하고, 인기순 조회 메서드는 호출하지 않는다")
        void latestSortCallsLatestRepositoryMethod() {
            given(postRepository.findPostsByContentIdLatest(CONTENT_ID, null, null, 3))
                    .willReturn(List.of());
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of())).willReturn(List.of());

            postFeedService.getFeed(CONTENT_ID, FeedSort.LATEST, null, 2);

            verify(postRepository).findPostsByContentIdLatest(CONTENT_ID, null, null, 3);
            verify(postRepository, never()).findPostsByContentIdPopular(any(), any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("리포지토리에는 항상 요청 size보다 1개 많이(size+1) 조회를 요청한다")
        void requestsOneMoreThanRequestedSize() {
            given(postRepository.findPostsByContentIdPopular(CONTENT_ID, null, null, null, 21))
                    .willReturn(List.of());
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of())).willReturn(List.of());

            postFeedService.getFeed(CONTENT_ID, FeedSort.POPULAR, null, 20);

            verify(postRepository).findPostsByContentIdPopular(CONTENT_ID, null, null, null, 21);
        }

        @Test
        @DisplayName("size보다 1개 더 많은 게시글이 오면 초과분은 응답에서 잘리고 hasNext=true, nextCursor는 잘린 마지막 게시글 기준이다")
        void trimsExtraItemAndReturnsNextCursor() {
            Post first = post(3L, 30, Instant.ofEpochMilli(3_000));
            Post second = post(2L, 20, Instant.ofEpochMilli(2_000));
            Post third = post(1L, 10, Instant.ofEpochMilli(1_000)); // size+1개 중 잘려나갈 항목
            List<Post> fetched = List.of(first, second, third);

            given(postRepository.findPostsByContentIdPopular(CONTENT_ID, null, null, null, 3))
                    .willReturn(fetched);
            given(postImageRepository.findByPostInOrderBySortOrderAsc(fetched)).willReturn(List.of(
                    PostImage.builder().post(first).imageUrl("https://img/3.jpg").sortOrder(0).build(),
                    PostImage.builder().post(second).imageUrl("https://img/2.jpg").sortOrder(0).build(),
                    PostImage.builder().post(third).imageUrl("https://img/1.jpg").sortOrder(0).build()
            ));

            CursorPageResponse<PostFeedItemResponse> response =
                    postFeedService.getFeed(CONTENT_ID, FeedSort.POPULAR, null, 2);

            assertThat(response.getContent())
                    .extracting(PostFeedItemResponse::postId)
                    .containsExactly(3L, 2L);
            assertThat(response.isHasNext()).isTrue();

            FeedCursor decoded = FeedCursor.decode(response.getNextCursor());
            assertThat(decoded.id()).isEqualTo(2L);
            assertThat(decoded.scrapCount()).isEqualTo(20);
            assertThat(decoded.createdAt()).isEqualTo(Instant.ofEpochMilli(2_000));
        }

        @Test
        @DisplayName("게시글이 size 이하로 오면 hasNext=false이고 nextCursor는 null이다")
        void returnsNoNextCursorWhenNoMorePosts() {
            Post only = post(1L, 10, Instant.ofEpochMilli(1_000));
            List<Post> fetched = List.of(only);

            given(postRepository.findPostsByContentIdPopular(CONTENT_ID, null, null, null, 3))
                    .willReturn(fetched);
            given(postImageRepository.findByPostInOrderBySortOrderAsc(fetched)).willReturn(List.of(
                    PostImage.builder().post(only).imageUrl("https://img/1.jpg").sortOrder(0).build()
            ));

            CursorPageResponse<PostFeedItemResponse> response =
                    postFeedService.getFeed(CONTENT_ID, FeedSort.POPULAR, null, 2);

            assertThat(response.getContent()).extracting(PostFeedItemResponse::postId).containsExactly(1L);
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("게시글이 하나도 없으면 빈 content와 hasNext=false를 반환한다")
        void returnsEmptyResponseWhenNoPosts() {
            given(postRepository.findPostsByContentIdPopular(CONTENT_ID, null, null, null, 3))
                    .willReturn(List.of());
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of())).willReturn(List.of());

            CursorPageResponse<PostFeedItemResponse> response =
                    postFeedService.getFeed(CONTENT_ID, FeedSort.POPULAR, null, 2);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("커서가 주어지면 디코딩한 필드를 그대로 리포지토리에 전달한다")
        void decodesCursorAndPassesFieldsToRepository() {
            FeedCursor cursor = new FeedCursor(15, Instant.ofEpochMilli(5_000), 42L);
            given(postRepository.findPostsByContentIdPopular(CONTENT_ID, 15, Instant.ofEpochMilli(5_000), 42L, 3))
                    .willReturn(List.of());
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of())).willReturn(List.of());

            postFeedService.getFeed(CONTENT_ID, FeedSort.POPULAR, cursor.encode(), 2);

            verify(postRepository).findPostsByContentIdPopular(CONTENT_ID, 15, Instant.ofEpochMilli(5_000), 42L, 3);
        }

        @Test
        @DisplayName("잘못된 커서 문자열이면 예외를 던지고 리포지토리를 호출하지 않는다")
        void throwsWhenCursorIsInvalid() {
            assertThatThrownBy(() -> postFeedService.getFeed(CONTENT_ID, FeedSort.POPULAR, "not-a-valid-cursor!!", 2))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository, postImageRepository);
        }

        @Test
        @DisplayName("게시글마다 sort_order가 가장 앞선 이미지를 대표 이미지로 채운다")
        void fillsRepresentativeImagePerPost() {
            Post postWithImages = post(1L, 10, Instant.ofEpochMilli(1_000));
            List<Post> fetched = List.of(postWithImages);

            given(postRepository.findPostsByContentIdPopular(CONTENT_ID, null, null, null, 3))
                    .willReturn(fetched);
            given(postImageRepository.findByPostInOrderBySortOrderAsc(fetched)).willReturn(List.of(
                    PostImage.builder().post(postWithImages).imageUrl("https://img/first.jpg").sortOrder(0).build(),
                    PostImage.builder().post(postWithImages).imageUrl("https://img/second.jpg").sortOrder(1).build()
            ));

            CursorPageResponse<PostFeedItemResponse> response =
                    postFeedService.getFeed(CONTENT_ID, FeedSort.POPULAR, null, 2);

            assertThat(response.getContent().get(0).imageUrl()).isEqualTo("https://img/first.jpg");
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
