package com.maggu.maggu.map.controller;

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.i18n.RequestLocaleResolver;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.dto.AutocompleteCandidateResponse;
import com.maggu.maggu.map.dto.MapSpotDetail;
import com.maggu.maggu.map.service.MapSearchService;
import com.maggu.maggu.post.dto.enums.FeedSort;
import com.maggu.maggu.post.dto.response.PostFeedItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MapSearchController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(RequestLocaleResolver.class)
@AutoConfigureMockMvc(addFilters = false)
class MapSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MapSearchService mapSearchService;

    @Nested
    @DisplayName("GET /api/v1/map/search/autocomplete")
    class GetAutocompleteCandidates {

        @Test
        @DisplayName("정상 keyword로 요청하면 200과 함께 자동완성 후보 목록을 반환한다")
        void returnsAutocompleteCandidates() throws Exception {
            AutocompleteCandidateResponse candidate = AutocompleteCandidateResponse.builder()
                    .contentId("126234")
                    .contentType(ContentType.TOURIST_ATTRACTION)
                    .title("해운대해수욕장")
                    .build();
            given(mapSearchService.getAutocompleteCandidates("해운대", AppLocale.KO)).willReturn(List.of(candidate));

            mockMvc.perform(get("/api/v1/map/search/autocomplete")
                            .param("keyword", "해운대"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].contentId").value("126234"))
                    .andExpect(jsonPath("$.data[0].contentType").value(ContentType.TOURIST_ATTRACTION.getId()))
                    .andExpect(jsonPath("$.data[0].title").value("해운대해수욕장"));
        }

        @Test
        @DisplayName("필수 파라미터(keyword)가 없으면 서비스는 호출하지 않는다")
        void doesNotCallServiceWhenKeywordMissing() throws Exception {
            mockMvc.perform(get("/api/v1/map/search/autocomplete"));

            verifyNoInteractions(mapSearchService);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/map/search/posts")
    class GetPosts {

        @Test
        @DisplayName("정상 요청이면 200과 함께 커서 기반 게시글 목록을 반환한다")
        void returnsPosts() throws Exception {
            CursorPageResponse<PostFeedItemResponse> response = CursorPageResponse.<PostFeedItemResponse>builder()
                    .content(List.of(new PostFeedItemResponse(1L, "https://img/a.jpg")))
                    .nextCursor("next-cursor")
                    .hasNext(true)
                    .build();
            given(mapSearchService.searchPosts("해운대", FeedSort.POPULAR, null, 20)).willReturn(response);

            mockMvc.perform(get("/api/v1/map/search/posts")
                            .param("keyword", "해운대")
                            .param("feedSort", "POPULAR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].postId").value(1))
                    .andExpect(jsonPath("$.data.content[0].imageUrl").value("https://img/a.jpg"))
                    .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"))
                    .andExpect(jsonPath("$.data.hasNext").value(true));
        }

        @Test
        @DisplayName("cursor 파라미터 없이 요청하면 첫 페이지로 간주해 서비스에 null cursor를 전달한다")
        void firstPageRequestPassesNullCursor() throws Exception {
            given(mapSearchService.searchPosts("해운대", FeedSort.LATEST, null, 20))
                    .willReturn(CursorPageResponse.<PostFeedItemResponse>builder()
                            .content(List.of()).nextCursor(null).hasNext(false).build());

            mockMvc.perform(get("/api/v1/map/search/posts")
                            .param("keyword", "해운대")
                            .param("feedSort", "LATEST"))
                    .andExpect(status().isOk());

            verify(mapSearchService).searchPosts("해운대", FeedSort.LATEST, null, 20);
        }

        @Test
        @DisplayName("size가 최대치(100)를 넘으면 100으로 clamp해서 서비스에 전달한다")
        void clampsSizeToMax() throws Exception {
            given(mapSearchService.searchPosts("해운대", FeedSort.POPULAR, null, 100))
                    .willReturn(CursorPageResponse.<PostFeedItemResponse>builder()
                            .content(List.of()).nextCursor(null).hasNext(false).build());

            mockMvc.perform(get("/api/v1/map/search/posts")
                            .param("keyword", "해운대")
                            .param("feedSort", "POPULAR")
                            .param("size", "500"))
                    .andExpect(status().isOk());

            verify(mapSearchService).searchPosts("해운대", FeedSort.POPULAR, null, 100);
        }

        @Test
        @DisplayName("필수 파라미터(keyword)가 없으면 서비스는 호출하지 않는다")
        void doesNotCallServiceWhenKeywordMissing() throws Exception {
            mockMvc.perform(get("/api/v1/map/search/posts")
                    .param("feedSort", "POPULAR"));

            verifyNoInteractions(mapSearchService);
        }

        @Test
        @DisplayName("정의되지 않은 feedSort 값이면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenFeedSortIsInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/map/search/posts")
                            .param("keyword", "해운대")
                            .param("feedSort", "INVALID"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(mapSearchService);
        }

        @Test
        @DisplayName("서비스에서 BusinessException이 발생하면 해당 에러코드로 응답한다")
        void returnsErrorBodyWhenServiceThrowsBusinessException() throws Exception {
            given(mapSearchService.searchPosts("해운대", FeedSort.POPULAR, null, 20))
                    .willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

            mockMvc.perform(get("/api/v1/map/search/posts")
                            .param("keyword", "해운대")
                            .param("feedSort", "POPULAR"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON-001"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/map/search/spots")
    class GetSpots {

        @Test
        @DisplayName("정상 keyword로 요청하면 200과 함께 장소 상세 목록을 반환한다")
        void returnsSpots() throws Exception {
            MapSpotDetail detail = new MapSpotDetail(
                    "126234", ContentType.TOURIST_ATTRACTION, "051-749-4062", "해운대해수욕장",
                    "부산 해운대구 해운대해변로 264", List.of("https://img/a.jpg"),
                    "09:00~18:00", null, null, 129.16, 35.16);
            given(mapSearchService.searchSpots("해운대", AppLocale.KO)).willReturn(List.of(detail));

            mockMvc.perform(get("/api/v1/map/search/spots")
                            .param("keyword", "해운대"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].contentId").value("126234"))
                    .andExpect(jsonPath("$.data[0].title").value("해운대해수욕장"))
                    .andExpect(jsonPath("$.data[0].tel").value("051-749-4062"));
        }

        @Test
        @DisplayName("필수 파라미터(keyword)가 없으면 서비스는 호출하지 않는다")
        void doesNotCallServiceWhenKeywordMissing() throws Exception {
            mockMvc.perform(get("/api/v1/map/search/spots"));

            verifyNoInteractions(mapSearchService);
        }

        @Test
        @DisplayName("서비스에서 BusinessException이 발생하면 해당 에러코드로 응답한다")
        void returnsErrorBodyWhenServiceThrowsBusinessException() throws Exception {
            given(mapSearchService.searchSpots("해운대", AppLocale.KO))
                    .willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

            mockMvc.perform(get("/api/v1/map/search/spots")
                            .param("keyword", "해운대"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON-001"));
        }
    }
}
