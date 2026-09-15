package com.maggu.maggu.map.service;

import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.community.entity.PostImage;
import com.maggu.maggu.community.repository.PostImageRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.map.cache.TourSpotCache;
import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.client.TourSpot;
import com.maggu.maggu.map.dto.AutocompleteCandidateResponse;
import com.maggu.maggu.map.dto.MapSpotDetail;
import com.maggu.maggu.post.dto.enums.FeedSort;
import com.maggu.maggu.post.dto.response.PostFeedItemResponse;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.post.repository.PostRepository;
import com.maggu.maggu.post.service.FeedCursor;
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
class MapSearchServiceTest {

    private static final String KEYWORD = "해운대";

    @Mock
    private TourSpotCache tourSpotCache;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private MapService mapService;

    @InjectMocks
    private MapSearchService mapSearchService;

    @Nested
    @DisplayName("getAutocompleteCandidates")
    class GetAutocompleteCandidates {

        private static final int AUTOCOMPLETE_MAX_RESULTS = 6;

        @Test
        @DisplayName("캐시에서 매칭된 스팟을 자동완성 후보 응답으로 변환해 반환한다")
        void returnsCandidatesConvertedFromMatchedSpots() {
            TourSpot spot = new TourSpot("126234", ContentType.TOURIST_ATTRACTION, "해운대해수욕장", 129.16, 35.16);
            given(tourSpotCache.findByKeyword(KEYWORD, AUTOCOMPLETE_MAX_RESULTS)).willReturn(List.of(spot));

            List<AutocompleteCandidateResponse> result = mapSearchService.getAutocompleteCandidates(KEYWORD);

            assertThat(result).hasSize(1);
            AutocompleteCandidateResponse candidate = result.get(0);
            assertThat(candidate.contentId()).isEqualTo("126234");
            assertThat(candidate.contentType()).isEqualTo(ContentType.TOURIST_ATTRACTION);
            assertThat(candidate.title()).isEqualTo("해운대해수욕장");
        }

        @Test
        @DisplayName("캐시에 매칭되는 스팟이 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoSpotsMatch() {
            given(tourSpotCache.findByKeyword("존재하지않는키워드", AUTOCOMPLETE_MAX_RESULTS)).willReturn(List.of());

            List<AutocompleteCandidateResponse> result = mapSearchService.getAutocompleteCandidates("존재하지않는키워드");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("자동완성 최대 개수(6개)로 캐시를 조회한다")
        void queriesCacheWithAutocompleteMaxResults() {
            given(tourSpotCache.findByKeyword(KEYWORD, AUTOCOMPLETE_MAX_RESULTS)).willReturn(List.of());

            mapSearchService.getAutocompleteCandidates(KEYWORD);

            verify(tourSpotCache).findByKeyword(KEYWORD, AUTOCOMPLETE_MAX_RESULTS);
        }
    }

    @Nested
    @DisplayName("getPosts")
    class GetPosts {

        @Test
        @DisplayName("POPULAR 정렬이면 인기순 조회 메서드를 호출하고, 최신순 조회 메서드는 호출하지 않는다")
        void popularSortCallsPopularRepositoryMethod() {
            given(postRepository.findByKeywordPopular(KEYWORD, null, null, null, 3))
                    .willReturn(List.of());
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of())).willReturn(List.of());

            mapSearchService.searchPosts(KEYWORD, FeedSort.POPULAR, null, 2);

            verify(postRepository).findByKeywordPopular(KEYWORD, null, null, null, 3);
            verify(postRepository, never()).findByKeywordLatest(any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("LATEST 정렬이면 최신순 조회 메서드를 호출하고, 인기순 조회 메서드는 호출하지 않는다")
        void latestSortCallsLatestRepositoryMethod() {
            given(postRepository.findByKeywordLatest(KEYWORD, null, null, 3))
                    .willReturn(List.of());
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of())).willReturn(List.of());

            mapSearchService.searchPosts(KEYWORD, FeedSort.LATEST, null, 2);

            verify(postRepository).findByKeywordLatest(KEYWORD, null, null, 3);
            verify(postRepository, never()).findByKeywordPopular(any(), any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("리포지토리에는 항상 요청 size보다 1개 많이(size+1) 조회를 요청한다")
        void requestsOneMoreThanRequestedSize() {
            given(postRepository.findByKeywordPopular(KEYWORD, null, null, null, 21))
                    .willReturn(List.of());
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of())).willReturn(List.of());

            mapSearchService.searchPosts(KEYWORD, FeedSort.POPULAR, null, 20);

            verify(postRepository).findByKeywordPopular(KEYWORD, null, null, null, 21);
        }

        @Test
        @DisplayName("size보다 1개 더 많은 게시글이 오면 초과분은 응답에서 잘리고 hasNext=true, nextCursor는 잘린 마지막 게시글 기준이다")
        void trimsExtraItemAndReturnsNextCursor() {
            Post first = post(3L, 30, Instant.ofEpochMilli(3_000));
            Post second = post(2L, 20, Instant.ofEpochMilli(2_000));
            Post third = post(1L, 10, Instant.ofEpochMilli(1_000)); // size+1개 중 잘려나갈 항목
            List<Post> fetched = List.of(first, second, third);

            given(postRepository.findByKeywordPopular(KEYWORD, null, null, null, 3))
                    .willReturn(fetched);
            given(postImageRepository.findByPostInOrderBySortOrderAsc(fetched)).willReturn(List.of(
                    PostImage.builder().post(first).imageUrl("https://img/3.jpg").sortOrder(0).build(),
                    PostImage.builder().post(second).imageUrl("https://img/2.jpg").sortOrder(0).build(),
                    PostImage.builder().post(third).imageUrl("https://img/1.jpg").sortOrder(0).build()
            ));

            CursorPageResponse<PostFeedItemResponse> response =
                    mapSearchService.searchPosts(KEYWORD, FeedSort.POPULAR, null, 2);

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

            given(postRepository.findByKeywordPopular(KEYWORD, null, null, null, 3))
                    .willReturn(fetched);
            given(postImageRepository.findByPostInOrderBySortOrderAsc(fetched)).willReturn(List.of(
                    PostImage.builder().post(only).imageUrl("https://img/1.jpg").sortOrder(0).build()
            ));

            CursorPageResponse<PostFeedItemResponse> response =
                    mapSearchService.searchPosts(KEYWORD, FeedSort.POPULAR, null, 2);

            assertThat(response.getContent()).extracting(PostFeedItemResponse::postId).containsExactly(1L);
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("게시글이 하나도 없으면 빈 content와 hasNext=false를 반환한다")
        void returnsEmptyResponseWhenNoPosts() {
            given(postRepository.findByKeywordPopular(KEYWORD, null, null, null, 3))
                    .willReturn(List.of());
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of())).willReturn(List.of());

            CursorPageResponse<PostFeedItemResponse> response =
                    mapSearchService.searchPosts(KEYWORD, FeedSort.POPULAR, null, 2);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("커서가 주어지면 디코딩한 필드를 그대로 리포지토리에 전달한다")
        void decodesCursorAndPassesFieldsToRepository() {
            FeedCursor cursor = new FeedCursor(15, Instant.ofEpochMilli(5_000), 42L);
            given(postRepository.findByKeywordPopular(KEYWORD, 15, Instant.ofEpochMilli(5_000), 42L, 3))
                    .willReturn(List.of());
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of())).willReturn(List.of());

            mapSearchService.searchPosts(KEYWORD, FeedSort.POPULAR, cursor.encode(), 2);

            verify(postRepository).findByKeywordPopular(KEYWORD, 15, Instant.ofEpochMilli(5_000), 42L, 3);
        }

        @Test
        @DisplayName("잘못된 커서 문자열이면 예외를 던지고 리포지토리를 호출하지 않는다")
        void throwsWhenCursorIsInvalid() {
            assertThatThrownBy(() -> mapSearchService.searchPosts(KEYWORD, FeedSort.POPULAR, "not-a-valid-cursor!!", 2))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository, postImageRepository);
        }

        @Test
        @DisplayName("게시글마다 sort_order가 가장 앞선 이미지를 대표 이미지로 채운다")
        void fillsRepresentativeImagePerPost() {
            Post postWithImages = post(1L, 10, Instant.ofEpochMilli(1_000));
            List<Post> fetched = List.of(postWithImages);

            given(postRepository.findByKeywordPopular(KEYWORD, null, null, null, 3))
                    .willReturn(fetched);
            given(postImageRepository.findByPostInOrderBySortOrderAsc(fetched)).willReturn(List.of(
                    PostImage.builder().post(postWithImages).imageUrl("https://img/first.jpg").sortOrder(0).build(),
                    PostImage.builder().post(postWithImages).imageUrl("https://img/second.jpg").sortOrder(1).build()
            ));

            CursorPageResponse<PostFeedItemResponse> response =
                    mapSearchService.searchPosts(KEYWORD, FeedSort.POPULAR, null, 2);

            assertThat(response.getContent().get(0).imageUrl()).isEqualTo("https://img/first.jpg");
        }
    }

    @Nested
    @DisplayName("searchSpots")
    class SearchSpots {

        private static final int SPOTS_MAX_RESULTS = 10;

        @Test
        @DisplayName("캐시에서 매칭된 스팟마다 상세를 조회해 리스트로 반환한다")
        void returnsSpotDetailsForMatchedSpots() {
            TourSpot first = TourSpot.builder().contentId("126234").contentType(ContentType.TOURIST_ATTRACTION)
                    .title("해운대해수욕장").mapX(129.16).mapY(35.16).build();
            TourSpot second = TourSpot.builder().contentId("126235").contentType(ContentType.RESTAURANT)
                    .title("해운대암소갈비집").mapX(129.17).mapY(35.17).build();
            given(tourSpotCache.findByKeyword(KEYWORD, SPOTS_MAX_RESULTS)).willReturn(List.of(first, second));

            MapSpotDetail firstDetail = mapSpotDetail("126234", "해운대해수욕장");
            MapSpotDetail secondDetail = mapSpotDetail("126235", "해운대암소갈비집");
            given(mapService.getMapSpotDetail("126234")).willReturn(firstDetail);
            given(mapService.getMapSpotDetail("126235")).willReturn(secondDetail);

            List<MapSpotDetail> result = mapSearchService.searchSpots(KEYWORD);

            assertThat(result).containsExactly(firstDetail, secondDetail);
        }

        @Test
        @DisplayName("캐시에 매칭되는 스팟이 없으면 빈 리스트를 반환하고 상세 조회는 호출하지 않는다")
        void returnsEmptyListWhenNoSpotsMatch() {
            given(tourSpotCache.findByKeyword(KEYWORD, SPOTS_MAX_RESULTS)).willReturn(List.of());

            List<MapSpotDetail> result = mapSearchService.searchSpots(KEYWORD);

            assertThat(result).isEmpty();
            verifyNoInteractions(mapService);
        }

        @Test
        @DisplayName("장소 검색 최대 개수(10개)로 캐시를 조회한다")
        void queriesCacheWithSpotsMaxResults() {
            given(tourSpotCache.findByKeyword(KEYWORD, SPOTS_MAX_RESULTS)).willReturn(List.of());

            mapSearchService.searchSpots(KEYWORD);

            verify(tourSpotCache).findByKeyword(KEYWORD, SPOTS_MAX_RESULTS);
        }

        @Test
        @DisplayName("상세 조회 중 특정 장소가 MAP_CONTENT_NOT_FOUND면 그 장소만 건너뛰고 나머지는 반환한다")
        void skipsSpotWhenNotFoundAndReturnsRest() {
            TourSpot missing = TourSpot.builder().contentId("999").contentType(ContentType.TOURIST_ATTRACTION)
                    .title("해운대사라진곳").mapX(129.0).mapY(35.0).build();
            TourSpot found = TourSpot.builder().contentId("126234").contentType(ContentType.TOURIST_ATTRACTION)
                    .title("해운대해수욕장").mapX(129.16).mapY(35.16).build();
            given(tourSpotCache.findByKeyword(KEYWORD, SPOTS_MAX_RESULTS)).willReturn(List.of(missing, found));

            given(mapService.getMapSpotDetail("999")).willThrow(new BusinessException(ErrorCode.MAP_CONTENT_NOT_FOUND));
            MapSpotDetail foundDetail = mapSpotDetail("126234", "해운대해수욕장");
            given(mapService.getMapSpotDetail("126234")).willReturn(foundDetail);

            List<MapSpotDetail> result = mapSearchService.searchSpots(KEYWORD);

            assertThat(result).containsExactly(foundDetail);
        }

        @Test
        @DisplayName("상세 조회 중 MAP_CONTENT_NOT_FOUND가 아닌 BusinessException이 발생하면 그대로 전파한다")
        void propagatesOtherBusinessExceptions() {
            TourSpot spot = TourSpot.builder().contentId("126234").contentType(ContentType.TOURIST_ATTRACTION)
                    .title("해운대해수욕장").mapX(129.16).mapY(35.16).build();
            given(tourSpotCache.findByKeyword(KEYWORD, SPOTS_MAX_RESULTS)).willReturn(List.of(spot));
            given(mapService.getMapSpotDetail("126234"))
                    .willThrow(new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR));

            assertThatThrownBy(() -> mapSearchService.searchSpots(KEYWORD))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_TOURISM_API_ERROR));
        }

        @Test
        @DisplayName("keyword가 null이거나 공백뿐이면 예외를 던지고 캐시/상세 조회를 호출하지 않는다")
        void throwsWhenKeywordIsBlank() {
            for (String blank : new String[]{null, "", " ", "   "}) {
                assertThatThrownBy(() -> mapSearchService.searchSpots(blank))
                        .isInstanceOfSatisfying(BusinessException.class,
                                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
            }

            verifyNoInteractions(tourSpotCache, mapService);
        }

        @Test
        @DisplayName("keyword 앞뒤 공백은 trim한 뒤 캐시를 조회한다")
        void trimsKeywordBeforeQueryingCache() {
            given(tourSpotCache.findByKeyword(KEYWORD, SPOTS_MAX_RESULTS)).willReturn(List.of());

            mapSearchService.searchSpots(" " + KEYWORD + " ");

            verify(tourSpotCache).findByKeyword(KEYWORD, SPOTS_MAX_RESULTS);
        }

        private MapSpotDetail mapSpotDetail(String contentId, String title) {
            return new MapSpotDetail(contentId, ContentType.TOURIST_ATTRACTION, "051-749-4062", title,
                    "부산 해운대구", List.of("https://img/a.jpg"), "09:00~18:00", null, null, 129.16, 35.16);
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
