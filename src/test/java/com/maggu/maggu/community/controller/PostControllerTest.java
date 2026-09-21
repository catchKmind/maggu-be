package com.maggu.maggu.community.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.community.dto.request.PostCreateRequest;
import com.maggu.maggu.community.dto.response.PostCreateResponse;
import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.post.dto.enums.FeedSort;
import com.maggu.maggu.post.dto.response.PostFeedItemResponse;
import com.maggu.maggu.community.service.PostCommandService;
import com.maggu.maggu.community.service.PostFeedService;
import com.maggu.maggu.community.service.PostQueryService;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.global.security.CustomUserDetails;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// POST /posts 작성과 GET /feed 관광지 피드를 다룬다.
@WebMvcTest(controllers = PostController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostQueryService queryService;

    @MockitoBean
    private PostCommandService commandService;

    @MockitoBean
    private PostFeedService feedService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/v1/community/posts")
    class CreatePost {

        @Test
        @DisplayName("정상 요청이면 생성된 게시글 ID를 반환한다")
        void createsPost() throws Exception {
            authenticateAs(appUser(1L));
            given(commandService.createPost(any(), any())).willReturn(
                    PostCreateResponse.builder().postId(10L).slug("abc123").build());

            mockMvc.perform(post("/api/v1/community/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PostCreateRequest("해운대 다녀왔어요", PostCategory.RECOMMEND,
                                            null, null, null, null, null, null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.postId").value(10))
                    .andExpect(jsonPath("$.data.slug").value("abc123"));
        }

        @Test
        @DisplayName("본문이 없으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenContentBlank() throws Exception {
            authenticateAs(appUser(1L));

            mockMvc.perform(post("/api/v1/community/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content":"","category":"RECOMMEND"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(commandService);
        }

        @Test
        @DisplayName("카테고리가 없으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenCategoryMissing() throws Exception {
            authenticateAs(appUser(1L));

            mockMvc.perform(post("/api/v1/community/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content":"해운대 다녀왔어요"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(commandService);
        }

        @Test
        @DisplayName("사진이 있는데 위치가 없으면 서비스 예외를 그대로 반환한다")
        void returnsBadRequestWhenLocationRequired() throws Exception {
            authenticateAs(appUser(1L));
            willThrow(new BusinessException(ErrorCode.POST_LOCATION_REQUIRED))
                    .given(commandService).createPost(any(), any());

            mockMvc.perform(post("/api/v1/community/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PostCreateRequest("본문", PostCategory.RECOMMEND,
                                            List.of("https://img/a.jpg"), null, null, null, null, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("POST-004"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/community/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PostCreateRequest("본문", PostCategory.RECOMMEND,
                                            null, null, null, null, null, null))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(commandService);
        }
    }

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
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

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
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON-001"));
        }
    }

    private void authenticateAs(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, List.of()));
    }

    private AppUser appUser(Long id) {
        AppUser user = AppUser.builder()
                .provider(Provider.APPLE)
                .providerUserId("apple-uid")
                .email("test@test.com")
                .nickname("나그네")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
