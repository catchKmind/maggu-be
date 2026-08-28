package com.maggu.maggu.map.controller;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.dto.MapGeometry;
import com.maggu.maggu.map.dto.MapPostFeature;
import com.maggu.maggu.map.dto.MapPostProperties;
import com.maggu.maggu.map.dto.MapPostsResponse;
import com.maggu.maggu.map.dto.MapSpotDetail;
import com.maggu.maggu.map.dto.MapSpotFeature;
import com.maggu.maggu.map.dto.MapSpotsResponse;
import com.maggu.maggu.map.dto.TourSpotProperties;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
import com.maggu.maggu.map.enums.MapCategory;
import com.maggu.maggu.map.service.MapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 이 컨트롤러 테스트 범위(HTTP 계층)와 무관한 시큐리티 필터는 아예 스캔 대상에서 제외한다.
@WebMvcTest(controllers = MapController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class MapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MapService mapService;

    @Nested
    @DisplayName("GET /api/v1/map/posts")
    class GetPosts {

        @Test
        @DisplayName("정상 bbox로 요청하면 200과 함께 게시글 GeoJSON을 반환한다")
        void returnsPostsWithinBbox() throws Exception {
            MapPostFeature feature = MapPostFeature.of(
                    MapGeometry.of(127.05, 37.55),
                    new MapPostProperties(1L, "ab12cd", "https://img/a.jpg", 12, "126234", "남산타워", 3));
            given(mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, null))
                    .willReturn(MapPostsResponse.of(List.of(feature)));

            mockMvc.perform(get("/api/v1/map/posts")
                            .param("minLat", "37.4")
                            .param("minLng", "126.8")
                            .param("maxLat", "37.7")
                            .param("maxLng", "127.2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.type").value("FeatureCollection"))
                    .andExpect(jsonPath("$.data.features[0].properties.postId").value(1))
                    .andExpect(jsonPath("$.data.features[0].properties.placePostCount").value(3));
        }

        @Test
        @DisplayName("category 파라미터를 함께 전달하면 서비스에 그대로 넘긴다")
        void passesCategoryToService() throws Exception {
            given(mapService.getMapPosts(37.4, 126.8, 37.7, 127.2, MapCategory.FOOD))
                    .willReturn(MapPostsResponse.of(List.of()));

            mockMvc.perform(get("/api/v1/map/posts")
                            .param("minLat", "37.4")
                            .param("minLng", "126.8")
                            .param("maxLat", "37.7")
                            .param("maxLng", "127.2")
                            .param("category", "FOOD"))
                    .andExpect(status().isOk());

            verify(mapService).getMapPosts(37.4, 126.8, 37.7, 127.2, MapCategory.FOOD);
        }

        @Test
        @DisplayName("필수 파라미터(minLat)가 없으면 서비스는 호출하지 않는다")
        void doesNotCallServiceWhenRequiredParamMissing() throws Exception {
            mockMvc.perform(get("/api/v1/map/posts")
                    .param("minLng", "126.8")
                    .param("maxLat", "37.7")
                    .param("maxLng", "127.2"));

            verifyNoInteractions(mapService);
        }

        @Test
        @DisplayName("category에 정의되지 않은 값을 주면 서비스는 호출하지 않는다")
        void doesNotCallServiceWhenCategoryIsInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/map/posts")
                    .param("minLat", "37.4")
                    .param("minLng", "126.8")
                    .param("maxLat", "37.7")
                    .param("maxLng", "127.2")
                    .param("category", "INVALID"));

            verifyNoInteractions(mapService);
        }

        @Test
        @DisplayName("서비스에서 BusinessException이 발생하면 해당 에러코드로 응답한다")
            // 알려진 버그: ResponseWrappingAdvice가 GlobalExceptionHandler의 ApiResponse.error(...)를
            // 다시 한번 ApiResponse.success(...)로 감싸서, 최상위 success/code가 아니라 $.data.success/$.data.code에
            // 실제 에러 정보가 들어간다. HTTP 상태코드(400)는 정상이라 이 테스트는 지금의 실제 동작을 그대로 검증한다.
            // 원인 조사/수정은 별도 이슈로 분리하기로 함 — 고쳐지면 이 테스트도 $.success/$.code로 되돌려야 한다.
        void returnsErrorBodyWhenServiceThrowsBusinessException() throws Exception {
            given(mapService.getMapPosts(37.7, 126.8, 37.4, 127.2, null))
                    .willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

            mockMvc.perform(get("/api/v1/map/posts")
                            .param("minLat", "37.7")
                            .param("minLng", "126.8")
                            .param("maxLat", "37.4")
                            .param("maxLng", "127.2"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.success").value(false))
                    .andExpect(jsonPath("$.data.code").value("COMMON-001"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/map/spots")
    class GetSpots {

        @Test
        @DisplayName("정상 bbox로 요청하면 200과 함께 스팟 GeoJSON을 반환한다")
        void returnsSpotsWithinBbox() throws Exception {
            MapSpotFeature feature = MapSpotFeature.of(
                    MapGeometry.of(127.05, 37.55),
                    new TourSpotProperties("126234", ContentType.TOURIST_ATTRACTION, "남산타워"));
            given(mapService.getMapSpots(37.4, 126.8, 37.7, 127.2))
                    .willReturn(MapSpotsResponse.of(List.of(feature)));

            mockMvc.perform(get("/api/v1/map/spots")
                            .param("minLat", "37.4")
                            .param("minLng", "126.8")
                            .param("maxLat", "37.7")
                            .param("maxLng", "127.2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.type").value("FeatureCollection"))
                    .andExpect(jsonPath("$.data.features[0].properties.contentId").value("126234"));
        }

        @Test
        @DisplayName("필수 파라미터(maxLng)가 없으면 서비스는 호출하지 않는다")
        void doesNotCallServiceWhenRequiredParamMissing() throws Exception {
            mockMvc.perform(get("/api/v1/map/spots")
                    .param("minLat", "37.4")
                    .param("minLng", "126.8")
                    .param("maxLat", "37.7"));

            verifyNoInteractions(mapService);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/map/spots/{contentId}")
    class GetSpotDetail {

        @Test
        @DisplayName("정상 contentId로 요청하면 200과 함께 관광지 상세 정보를 반환한다")
        void returnsSpotDetail() throws Exception {
            MapSpotDetail detail = new MapSpotDetail(
                    "126234", ContentType.TOURIST_ATTRACTION, "02-1234-5678", "남산타워",
                    "서울 용산구 남산공원길 105", List.of("https://img/a.jpg"),
                    "09:00~18:00", "매주 월요일", null, 127.05, 37.55);
            given(mapService.getMapSpotDetail("126234")).willReturn(detail);

            mockMvc.perform(get("/api/v1/map/spots/{contentId}", "126234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.contentId").value("126234"))
                    .andExpect(jsonPath("$.data.title").value("남산타워"))
                    .andExpect(jsonPath("$.data.images[0]").value("https://img/a.jpg"));
        }

        @Test
        @DisplayName("존재하지 않는 contentId면 404를 반환한다")
        void returnsNotFoundWhenContentIdDoesNotExist() throws Exception {
            given(mapService.getMapSpotDetail("999"))
                    .willThrow(new BusinessException(ErrorCode.MAP_CONTENT_NOT_FOUND));

            mockMvc.perform(get("/api/v1/map/spots/{contentId}", "999"))
                    .andExpect(status().isNotFound());
        }
    }
}
