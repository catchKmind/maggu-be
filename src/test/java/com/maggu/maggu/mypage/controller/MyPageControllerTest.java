package com.maggu.maggu.mypage.controller;

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.security.CustomUserDetails;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.mypage.dto.response.MyCommentListResponse;
import com.maggu.maggu.mypage.dto.response.MyCommentListResponse.MyCommentResponse;
import com.maggu.maggu.mypage.dto.response.MyPostListResponse;
import com.maggu.maggu.mypage.dto.response.MyPostListResponse.MyPostResponse;
import com.maggu.maggu.mypage.service.MyActivityService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MyPageController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyActivityService myActivityService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/mypage/posts")
    class GetMyPosts {

        private static final String URL = "/api/v1/mypage/posts";

        @Test
        @DisplayName("내 글 목록과 글 수/댓글 수를 반환한다")
        void returnsMyPosts() throws Exception {
            authenticateAs(appUser(1L));
            given(myActivityService.getMyPosts(any(), isNull(), eq(20))).willReturn(new MyPostListResponse(
                    5L, 12L,
                    new CursorPageResponse<>(List.of(
                            new MyPostResponse(102L, "https://cdn.maggu.com/POST/1/a.png", "사진 있는 글"),
                            new MyPostResponse(101L, null, "사진 없는 글")
                    ), "next-cursor", true)));

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.postCount").value(5))
                    .andExpect(jsonPath("$.data.commentCount").value(12))
                    .andExpect(jsonPath("$.data.posts.content[0].postId").value(102))
                    .andExpect(jsonPath("$.data.posts.content[0].imageUrl").value("https://cdn.maggu.com/POST/1/a.png"))
                    .andExpect(jsonPath("$.data.posts.content[0].content").value("사진 있는 글"))
                    .andExpect(jsonPath("$.data.posts.content[1].imageUrl").doesNotExist())
                    .andExpect(jsonPath("$.data.posts.nextCursor").value("next-cursor"))
                    .andExpect(jsonPath("$.data.posts.hasNext").value(true));
        }

        @Test
        @DisplayName("cursor와 size를 서비스에 그대로 전달한다")
        void passesCursorAndSize() throws Exception {
            authenticateAs(appUser(1L));
            given(myActivityService.getMyPosts(any(), eq("abc"), eq(10))).willReturn(emptyPostResponse());

            mockMvc.perform(get(URL).param("cursor", "abc").param("size", "10"))
                    .andExpect(status().isOk());

            verify(myActivityService).getMyPosts(any(), eq("abc"), eq(10));
        }

        @Test
        @DisplayName("size가 100을 넘으면 100으로 보정한다")
        void clampsSizeToMax() throws Exception {
            authenticateAs(appUser(1L));
            given(myActivityService.getMyPosts(any(), isNull(), eq(100))).willReturn(emptyPostResponse());

            mockMvc.perform(get(URL).param("size", "500"))
                    .andExpect(status().isOk());

            verify(myActivityService).getMyPosts(any(), isNull(), eq(100));
        }

        @Test
        @DisplayName("size가 1보다 작으면 1로 보정한다")
        void clampsSizeToMin() throws Exception {
            authenticateAs(appUser(1L));
            given(myActivityService.getMyPosts(any(), isNull(), eq(1))).willReturn(emptyPostResponse());

            mockMvc.perform(get(URL).param("size", "0"))
                    .andExpect(status().isOk());

            verify(myActivityService).getMyPosts(any(), isNull(), eq(1));
        }

        @Test
        @DisplayName("잘못된 커서면 400을 반환한다")
        void returnsBadRequestWhenCursorInvalid() throws Exception {
            authenticateAs(appUser(1L));
            willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE))
                    .given(myActivityService).getMyPosts(any(), eq("broken"), anyInt());

            mockMvc.perform(get(URL).param("cursor", "broken"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(myActivityService);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mypage/comments")
    class GetMyComments {

        private static final String URL = "/api/v1/mypage/comments";

        @Test
        @DisplayName("내 댓글 목록과 글 수/댓글 수를 반환한다")
        void returnsMyComments() throws Exception {
            authenticateAs(appUser(1L));
            given(myActivityService.getMyComments(any(), isNull(), eq(20))).willReturn(new MyCommentListResponse(
                    5L, 12L,
                    new CursorPageResponse<>(List.of(
                            new MyCommentResponse(202L, 10L, "원글작성자", "답글", Instant.parse("2026-11-03T00:00:00Z"), true)
                    ), null, false)));

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.postCount").value(5))
                    .andExpect(jsonPath("$.data.commentCount").value(12))
                    .andExpect(jsonPath("$.data.comments.content[0].commentId").value(202))
                    .andExpect(jsonPath("$.data.comments.content[0].postId").value(10))
                    .andExpect(jsonPath("$.data.comments.content[0].postWriterNickname").value("원글작성자"))
                    .andExpect(jsonPath("$.data.comments.content[0].content").value("답글"))
                    .andExpect(jsonPath("$.data.comments.content[0].isReply").value(true))
                    .andExpect(jsonPath("$.data.comments.hasNext").value(false))
                    .andExpect(jsonPath("$.data.comments.nextCursor").doesNotExist());
        }

        @Test
        @DisplayName("cursor와 size를 서비스에 그대로 전달한다")
        void passesCursorAndSize() throws Exception {
            authenticateAs(appUser(1L));
            given(myActivityService.getMyComments(any(), eq("abc"), eq(10))).willReturn(emptyCommentResponse());

            mockMvc.perform(get(URL).param("cursor", "abc").param("size", "10"))
                    .andExpect(status().isOk());

            verify(myActivityService).getMyComments(any(), eq("abc"), eq(10));
        }

        @Test
        @DisplayName("size가 100을 넘으면 100으로 보정한다")
        void clampsSizeToMax() throws Exception {
            authenticateAs(appUser(1L));
            given(myActivityService.getMyComments(any(), isNull(), eq(100))).willReturn(emptyCommentResponse());

            mockMvc.perform(get(URL).param("size", "500"))
                    .andExpect(status().isOk());

            verify(myActivityService).getMyComments(any(), isNull(), eq(100));
        }

        @Test
        @DisplayName("size가 1보다 작으면 1로 보정한다")
        void clampsSizeToMin() throws Exception {
            authenticateAs(appUser(1L));
            given(myActivityService.getMyComments(any(), isNull(), eq(1))).willReturn(emptyCommentResponse());

            mockMvc.perform(get(URL).param("size", "-5"))
                    .andExpect(status().isOk());

            verify(myActivityService).getMyComments(any(), isNull(), eq(1));
        }

        @Test
        @DisplayName("잘못된 커서면 400을 반환한다")
        void returnsBadRequestWhenCursorInvalid() throws Exception {
            authenticateAs(appUser(1L));
            willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE))
                    .given(myActivityService).getMyComments(any(), eq("broken"), anyInt());

            mockMvc.perform(get(URL).param("cursor", "broken"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(myActivityService);
        }
    }

    private MyPostListResponse emptyPostResponse() {
        return new MyPostListResponse(0L, 0L, new CursorPageResponse<>(List.of(), null, false));
    }

    private MyCommentListResponse emptyCommentResponse() {
        return new MyCommentListResponse(0L, 0L, new CursorPageResponse<>(List.of(), null, false));
    }

    private void authenticateAs(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, List.of()));
    }

    private AppUser appUser(Long id) {
        AppUser user = AppUser.builder()
                .provider(Provider.APPLE)
                .providerUserId("apple-uid")
                .email("user@test.com")
                .nickname("시진")
                .locale(AppLocale.KO)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
