package com.maggu.maggu.community.controller;

import com.maggu.maggu.community.dto.enums.FeedSort;
import com.maggu.maggu.community.dto.response.PostFeedItemResponse;
import com.maggu.maggu.community.service.PostCommandService;
import com.maggu.maggu.community.service.PostFeedService;
import com.maggu.maggu.community.service.PostQueryService;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
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

// GET /feed 하나만 다룬다. 나머지 PostController 엔드포인트는 이 브랜치 작업 범위 밖이라 다루지 않는다.
@WebMvcTest(controllers = PostController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostQueryService queryService;

    @MockitoBean
    private PostCommandService commandService;

    @MockitoBean
    private PostFeedService feedService;

    @Nested
    @DisplayName("GET /api/v1/community/posts/feed")
    class GetFeedByContentId {

        @Test
        @DisplayName("정상 요청이면 200과 함께 커서 기반 게시글 목록을 반환한다")
        void returnsFeed() throws Exception {
            CursorPageResponse<PostFeedItemResponse> response = CursorPageResponse.<PostFeedItemResponse>builder()
                    .content(List.of(new PostFeedItemResponse(1L, "https://img/a.jpg")))
                    .nextCursor("next-cursor")
                    .hasNext(true)
                    .build();
            given(feedService.getFeed("126234", FeedSort.POPULAR, null, 20)).willReturn(response);

            mockMvc.perform(get("/api/v1/community/posts/feed")
                            .param("contentId", "126234")
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
            given(feedService.getFeed("126234", FeedSort.LATEST, null, 20))
                    .willReturn(CursorPageResponse.<PostFeedItemResponse>builder()
                            .content(List.of()).nextCursor(null).hasNext(false).build());

            mockMvc.perform(get("/api/v1/community/posts/feed")
                            .param("contentId", "126234")
                            .param("feedSort", "LATEST"))
                    .andExpect(status().isOk());

            verify(feedService).getFeed("126234", FeedSort.LATEST, null, 20);
        }

        @Test
        @DisplayName("size가 최대치(100)를 넘으면 100으로 clamp해서 서비스에 전달한다")
        void clampsSizeToMax() throws Exception {
            given(feedService.getFeed("126234", FeedSort.POPULAR, null, 100))
                    .willReturn(CursorPageResponse.<PostFeedItemResponse>builder()
                            .content(List.of()).nextCursor(null).hasNext(false).build());

            mockMvc.perform(get("/api/v1/community/posts/feed")
                            .param("contentId", "126234")
                            .param("feedSort", "POPULAR")
                            .param("size", "500"))
                    .andExpect(status().isOk());

            verify(feedService).getFeed("126234", FeedSort.POPULAR, null, 100);
        }

        @Test
        @DisplayName("필수 파라미터(contentId)가 없으면 서비스는 호출하지 않는다")
        void doesNotCallServiceWhenContentIdMissing() throws Exception {
            mockMvc.perform(get("/api/v1/community/posts/feed")
                    .param("feedSort", "POPULAR"));

            verifyNoInteractions(feedService);
        }

        @Test
        @DisplayName("정의되지 않은 feedSort 값이면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenFeedSortIsInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/community/posts/feed")
                            .param("contentId", "126234")
                            .param("feedSort", "INVALID"))
                    .andExpect(status().isBadRequest())
                    // 알려진 버그: ResponseWrappingAdvice가 에러 응답을 한번 더 감싸서
                    // 실제 에러 정보는 $.success/$.code가 아니라 $.data.success/$.data.code에 들어간다.
                    // (MapControllerTest의 동일 케이스와 같은 이유 — 별도 이슈로 분리됨)
                    .andExpect(jsonPath("$.data.success").value(false))
                    .andExpect(jsonPath("$.data.code").value("COMMON-001"));

            verifyNoInteractions(feedService);
        }

        @Test
        @DisplayName("서비스에서 BusinessException이 발생하면 해당 에러코드로 응답한다")
        void returnsErrorBodyWhenServiceThrowsBusinessException() throws Exception {
            given(feedService.getFeed("126234", FeedSort.POPULAR, "broken-cursor", 20))
                    .willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

            mockMvc.perform(get("/api/v1/community/posts/feed")
                            .param("contentId", "126234")
                            .param("feedSort", "POPULAR")
                            .param("cursor", "broken-cursor"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.success").value(false))
                    .andExpect(jsonPath("$.data.code").value("COMMON-001"));
        }
    }
}
