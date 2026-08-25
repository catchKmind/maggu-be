package com.maggu.maggu.map.service;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.map.cache.TourSpotCache;
import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.client.TourApiClient;
import com.maggu.maggu.map.client.TourSpot;
import com.maggu.maggu.map.dto.MapPostFeature;
import com.maggu.maggu.map.dto.MapPostsResponse;
import com.maggu.maggu.map.dto.MapSpotFeature;
import com.maggu.maggu.map.dto.MapSpotsResponse;
import com.maggu.maggu.map.enums.MapCategory;
import com.maggu.maggu.post.repository.MapPostProjection;
import com.maggu.maggu.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MapServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private TourSpotCache spotCache;

    @InjectMocks
    private MapService mapService;

    @Nested
    @DisplayName("getMapSpots")
    class GetMapSpots {

        @Test
        @DisplayName("bbox 안의 스팟을 GeoJSON FeatureCollection으로 변환해 반환한다")
        void getMapSpots() {
            TourSpot spot = new TourSpot("126234", ContentType.TOURIST_ATTRACTION, "남산타워", 127.05, 37.55);
            given(spotCache.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of(spot));

            MapSpotsResponse response = mapService.getMapSpots(37.4, 126.8, 37.7, 127.2);

            assertThat(response.type()).isEqualTo("FeatureCollection");
            assertThat(response.features()).hasSize(1);

            MapSpotFeature feature = response.features().get(0);
            assertThat(feature.type()).isEqualTo("Feature");
            assertThat(feature.geometry().type()).isEqualTo("Point");
            assertThat(feature.geometry().coordinates()).containsExactly(127.05, 37.55);
            assertThat(feature.properties().contentId()).isEqualTo("126234");
            assertThat(feature.properties().contentType()).isEqualTo(ContentType.TOURIST_ATTRACTION);
            assertThat(feature.properties().title()).isEqualTo("남산타워");
        }

        @Test
        @DisplayName("좌표는 캐시에 (경도, 위도, 경도, 위도) 순서로 전달한다")
        void passesLngLatInCorrectOrderToCache() {
            given(spotCache.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of());

            mapService.getMapSpots(37.4, 126.8, 37.7, 127.2);

            verify(spotCache).findInBbox(126.8, 37.4, 127.2, 37.7);
        }

        @Test
        @DisplayName("bbox 안에 스팟이 없으면 features가 빈 리스트인 응답을 반환한다")
        void returnsEmptyFeaturesWhenNoSpotsInBbox() {
            given(spotCache.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of());

            MapSpotsResponse response = mapService.getMapSpots(37.4, 126.8, 37.7, 127.2);

            assertThat(response.type()).isEqualTo("FeatureCollection");
            assertThat(response.features()).isEmpty();
        }

        @Test
        @DisplayName("minLat이 maxLat보다 크거나 같으면 예외를 던지고 조회하지 않는다")
        void throwsWhenMinLatNotLessThanMaxLat() {
            assertThatThrownBy(() -> mapService.getMapSpots(37.7, 126.8, 37.7, 127.2))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(spotCache);
        }

        @Test
        @DisplayName("위도가 범위(-90~90)를 벗어나면 예외를 던지고 조회하지 않는다")
        void throwsWhenLatOutOfRange() {
            assertThatThrownBy(() -> mapService.getMapSpots(-91, 126.8, 37.7, 127.2))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(spotCache);
        }
    }

    @Nested
    @DisplayName("getMapPosts")
    class GetMapPosts {

        @Test
        @DisplayName("bbox 안의 게시글을 GeoJSON FeatureCollection으로 변환해 반환한다")
        void getMapPosts() {
            MapPostProjection projection = projection(
                    1L, "ab12cd", 127.05, 37.55, 12, "126234", "남산타워", "https://img/a.jpg");
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7))
                    .willReturn(List.of(projection));

            MapPostsResponse response = mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, null);

            assertThat(response.type()).isEqualTo("FeatureCollection");
            assertThat(response.features()).hasSize(1);

            MapPostFeature feature = response.features().get(0);
            assertThat(feature.type()).isEqualTo("Feature");
            assertThat(feature.geometry().type()).isEqualTo("Point");
            assertThat(feature.geometry().coordinates()).containsExactly(127.05, 37.55);
            assertThat(feature.properties().postId()).isEqualTo(1L);
            assertThat(feature.properties().slug()).isEqualTo("ab12cd");
            assertThat(feature.properties().scrapCount()).isEqualTo(12);
            assertThat(feature.properties().tourismContentId()).isEqualTo("126234");
            assertThat(feature.properties().placeName()).isEqualTo("남산타워");
            assertThat(feature.properties().representativeImageUrl()).isEqualTo("https://img/a.jpg");

            verifyNoInteractions(tourApiClient);
        }

        @Test
        @DisplayName("좌표는 리포지토리에 (경도, 위도, 경도, 위도) 순서로 전달한다")
        void passesLngLatInCorrectOrderToRepository() {
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of());

            mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, null);

            verify(postRepository).findInBbox(126.8, 37.4, 127.2, 37.7);
        }

        @Test
        @DisplayName("대표 이미지가 없는 게시글은 representativeImageUrl이 null인 채로 반환된다")
        void representativeImageUrlCanBeNull() {
            MapPostProjection projection = projection(
                    1L, "ab12cd", 127.05, 37.55, 0, null, null, null);
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7))
                    .willReturn(List.of(projection));

            MapPostsResponse response = mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, null);

            assertThat(response.features().get(0).properties().representativeImageUrl()).isNull();
        }

        @Test
        @DisplayName("bbox 안에 게시글이 없으면 features가 빈 리스트인 응답을 반환한다")
        void returnsEmptyFeaturesWhenNoPostsInBbox() {
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of());

            MapPostsResponse response = mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, null);

            assertThat(response.type()).isEqualTo("FeatureCollection");
            assertThat(response.features()).isEmpty();
        }

        @Test
        @DisplayName("minLat이 maxLat보다 크거나 같으면 예외를 던지고 조회하지 않는다")
        void throwsWhenMinLatNotLessThanMaxLat() {
            assertThatThrownBy(() -> mapService.getMapPosts(37.7, 126.8, 37.7, 127.2, null))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("minLng이 maxLng보다 크거나 같으면 예외를 던지고 조회하지 않는다")
        void throwsWhenMinLngNotLessThanMaxLng() {
            assertThatThrownBy(() -> mapService.getMapPosts(37.4, 127.2, 37.7, 127.2, null))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("위도가 -90 미만이면 예외를 던지고 조회하지 않는다")
        void throwsWhenMinLatBelowRange() {
            assertThatThrownBy(() -> mapService.getMapPosts(-91, 126.8, 37.7, 127.2, null))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("위도가 90을 초과하면 예외를 던지고 조회하지 않는다")
        void throwsWhenMaxLatAboveRange() {
            assertThatThrownBy(() -> mapService.getMapPosts(37.4, 126.8, 91, 127.2, null))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("경도가 -180 미만이면 예외를 던지고 조회하지 않는다")
        void throwsWhenMinLngBelowRange() {
            assertThatThrownBy(() -> mapService.getMapPosts(37.4, -181, 37.7, 127.2, null))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("경도가 180을 초과하면 예외를 던지고 조회하지 않는다")
        void throwsWhenMaxLngAboveRange() {
            assertThatThrownBy(() -> mapService.getMapPosts(37.4, 126.8, 37.7, 181, null))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("category=POPULAR면 TourAPI 호출 없이 scrapCount 내림차순으로 정렬한다")
        void popularCategorySortsByScrapCountDescWithoutTourApiCall() {
            MapPostProjection low = projection(1L, "low", 127.05, 37.55, 3, null, null, null);
            MapPostProjection high = projection(2L, "high", 127.06, 37.56, 10, null, null, null);
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of(low, high));

            MapPostsResponse response = mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, MapCategory.POPULAR);

            assertThat(response.features())
                    .extracting(f -> f.properties().postId())
                    .containsExactly(2L, 1L);
            verifyNoInteractions(tourApiClient);
        }

        @Test
        @DisplayName("장소 카테고리 필터: 요청한 카테고리와 일치하는 장소의 게시글만 남긴다")
        void categoryFilterKeepsOnlyMatchingContentType() {
            MapPostProjection food = projection(1L, "food", 127.05, 37.55, 1, "111", "식당", null);
            MapPostProjection stay = projection(2L, "stay", 127.06, 37.56, 1, "222", "호텔", null);
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of(food, stay));
            given(tourApiClient.findContentType("111")).willReturn(Optional.of(ContentType.RESTAURANT));
            given(tourApiClient.findContentType("222")).willReturn(Optional.of(ContentType.ACCOMMODATION));

            MapPostsResponse response = mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, MapCategory.FOOD);

            assertThat(response.features())
                    .extracting(f -> f.properties().postId())
                    .containsExactly(1L);
        }

        @Test
        @DisplayName("장소 카테고리 필터: tourismContentId가 없는 게시글은 결과에서 제외된다")
        void categoryFilterExcludesPostsWithoutTourismContentId() {
            MapPostProjection noPlace = projection(1L, "noplace", 127.05, 37.55, 1, null, null, null);
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of(noPlace));

            MapPostsResponse response = mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, MapCategory.FOOD);

            assertThat(response.features()).isEmpty();
            verifyNoInteractions(tourApiClient);
        }

        @Test
        @DisplayName("장소 카테고리 필터: 같은 tourismContentId를 공유하는 게시글이 여러 개여도 TourAPI 조회는 한 번만 한다")
        void categoryFilterDeduplicatesTourApiLookups() {
            MapPostProjection a = projection(1L, "a", 127.05, 37.55, 1, "111", "식당", null);
            MapPostProjection b = projection(2L, "b", 127.06, 37.56, 1, "111", "식당", null);
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of(a, b));
            given(tourApiClient.findContentType("111")).willReturn(Optional.of(ContentType.RESTAURANT));

            mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, MapCategory.FOOD);

            verify(tourApiClient, times(1)).findContentType(eq("111"));
        }

        @Test
        @DisplayName("장소 카테고리 필터: TourAPI가 해당 contentId를 찾지 못하면(빈 Optional) 그 게시글은 제외되고 요청은 실패하지 않는다")
        void categoryFilterExcludesPostsWhoseContentIdIsNotFound() {
            MapPostProjection post = projection(1L, "a", 127.05, 37.55, 1, "999", "삭제된 곳", null);
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of(post));
            given(tourApiClient.findContentType("999")).willReturn(Optional.empty());

            MapPostsResponse response = mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, MapCategory.FOOD);

            assertThat(response.features()).isEmpty();
        }

        @Test
        @DisplayName("bbox 안에 게시글이 없으면 카테고리 필터여도 TourAPI를 호출하지 않는다")
        void categoryFilterSkipsTourApiCallWhenNoPostsInBbox() {
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of());

            mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, MapCategory.FOOD);

            verifyNoInteractions(tourApiClient);
        }

        // 카테고리 필터에서 걸러지는 게시글은 getSlug()/getLng() 등 일부 stub이 실제로 안 쓰일 수 있어 lenient 처리
        private MapPostProjection projection(Long postId, String slug, double lng, double lat, int scrapCount,
                                             String tourismContentId, String placeName, String representativeImageUrl) {
            MapPostProjection projection = mock(MapPostProjection.class);
            lenient().when(projection.getPostId()).thenReturn(postId);
            lenient().when(projection.getSlug()).thenReturn(slug);
            lenient().when(projection.getLng()).thenReturn(lng);
            lenient().when(projection.getLat()).thenReturn(lat);
            lenient().when(projection.getScrapCount()).thenReturn(scrapCount);
            lenient().when(projection.getTourismContentId()).thenReturn(tourismContentId);
            lenient().when(projection.getPlaceName()).thenReturn(placeName);
            lenient().when(projection.getRepresentativeImageUrl()).thenReturn(representativeImageUrl);
            return projection;
        }
    }
}
