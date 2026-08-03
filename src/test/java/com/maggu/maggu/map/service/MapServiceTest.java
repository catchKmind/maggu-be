package com.maggu.maggu.map.service;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.map.dto.MapPostFeature;
import com.maggu.maggu.map.dto.MapPostsResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MapServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private MapService mapService;

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

            MapPostsResponse response = mapService.getMapPosts(37.4, 126.8, 37.7, 127.2);

            assertThat(response.getType()).isEqualTo("FeatureCollection");
            assertThat(response.getFeatures()).hasSize(1);

            MapPostFeature feature = response.getFeatures().get(0);
            assertThat(feature.getType()).isEqualTo("Feature");
            assertThat(feature.getGeometry().getType()).isEqualTo("Point");
            assertThat(feature.getGeometry().getCoordinates()).containsExactly(127.05, 37.55);
            assertThat(feature.getProperties().getPostId()).isEqualTo(1L);
            assertThat(feature.getProperties().getSlug()).isEqualTo("ab12cd");
            assertThat(feature.getProperties().getScrapCount()).isEqualTo(12);
            assertThat(feature.getProperties().getTourismContentId()).isEqualTo("126234");
            assertThat(feature.getProperties().getPlaceName()).isEqualTo("남산타워");
            assertThat(feature.getProperties().getRepresentativeImageUrl()).isEqualTo("https://img/a.jpg");
        }

        @Test
        @DisplayName("좌표는 리포지토리에 (경도, 위도, 경도, 위도) 순서로 전달한다")
        void passesLngLatInCorrectOrderToRepository() {
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of());

            mapService.getMapPosts(37.4, 126.8, 37.7, 127.2);

            verify(postRepository).findInBbox(126.8, 37.4, 127.2, 37.7);
        }

        @Test
        @DisplayName("대표 이미지가 없는 게시글은 representativeImageUrl이 null인 채로 반환된다")
        void representativeImageUrlCanBeNull() {
            MapPostProjection projection = projection(
                    1L, "ab12cd", 127.05, 37.55, 0, null, null, null);
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7))
                    .willReturn(List.of(projection));

            MapPostsResponse response = mapService.getMapPosts(37.4, 126.8, 37.7, 127.2);

            assertThat(response.getFeatures().get(0).getProperties().getRepresentativeImageUrl()).isNull();
        }

        @Test
        @DisplayName("bbox 안에 게시글이 없으면 features가 빈 리스트인 응답을 반환한다")
        void returnsEmptyFeaturesWhenNoPostsInBbox() {
            given(postRepository.findInBbox(126.8, 37.4, 127.2, 37.7)).willReturn(List.of());

            MapPostsResponse response = mapService.getMapPosts(37.4, 126.8, 37.7, 127.2);

            assertThat(response.getType()).isEqualTo("FeatureCollection");
            assertThat(response.getFeatures()).isEmpty();
        }

        @Test
        @DisplayName("minLat이 maxLat보다 크거나 같으면 예외를 던지고 조회하지 않는다")
        void throwsWhenMinLatNotLessThanMaxLat() {
            assertThatThrownBy(() -> mapService.getMapPosts(37.7, 126.8, 37.7, 127.2))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("minLng이 maxLng보다 크거나 같으면 예외를 던지고 조회하지 않는다")
        void throwsWhenMinLngNotLessThanMaxLng() {
            assertThatThrownBy(() -> mapService.getMapPosts(37.4, 127.2, 37.7, 127.2))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("위도가 -90 미만이면 예외를 던지고 조회하지 않는다")
        void throwsWhenMinLatBelowRange() {
            assertThatThrownBy(() -> mapService.getMapPosts(-91, 126.8, 37.7, 127.2))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("위도가 90을 초과하면 예외를 던지고 조회하지 않는다")
        void throwsWhenMaxLatAboveRange() {
            assertThatThrownBy(() -> mapService.getMapPosts(37.4, 126.8, 91, 127.2))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("경도가 -180 미만이면 예외를 던지고 조회하지 않는다")
        void throwsWhenMinLngBelowRange() {
            assertThatThrownBy(() -> mapService.getMapPosts(37.4, -181, 37.7, 127.2))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        @Test
        @DisplayName("경도가 180을 초과하면 예외를 던지고 조회하지 않는다")
        void throwsWhenMaxLngAboveRange() {
            assertThatThrownBy(() -> mapService.getMapPosts(37.4, 126.8, 37.7, 181))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository);
        }

        private MapPostProjection projection(Long postId, String slug, double lng, double lat, int scrapCount,
                                              String tourismContentId, String placeName, String representativeImageUrl) {
            MapPostProjection projection = mock(MapPostProjection.class);
            given(projection.getPostId()).willReturn(postId);
            given(projection.getSlug()).willReturn(slug);
            given(projection.getLng()).willReturn(lng);
            given(projection.getLat()).willReturn(lat);
            given(projection.getScrapCount()).willReturn(scrapCount);
            given(projection.getTourismContentId()).willReturn(tourismContentId);
            given(projection.getPlaceName()).willReturn(placeName);
            given(projection.getRepresentativeImageUrl()).willReturn(representativeImageUrl);
            return projection;
        }
    }
}
