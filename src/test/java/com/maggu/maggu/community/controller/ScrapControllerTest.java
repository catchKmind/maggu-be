package com.maggu.maggu.community.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.community.dto.request.ScrapCreateRequest;
import com.maggu.maggu.community.dto.response.FolderResponse;
import com.maggu.maggu.community.dto.response.PageResponse;
import com.maggu.maggu.community.dto.response.PostSummaryResponse;
import com.maggu.maggu.community.dto.response.ScrapResponse;
import com.maggu.maggu.community.service.ScrapService;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ScrapController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class ScrapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScrapService scrapService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/v1/community/scraps")
    class CreateScrap {

        @Test
        @DisplayName("정상 요청이면 스크랩 결과를 반환한다")
        void scrapsPost() throws Exception {
            authenticateAs(appUser(1L));
            given(scrapService.scrap(any(), any())).willReturn(
                    ScrapResponse.builder().postId(10L).folderId(3L).scrapped(true).build());

            mockMvc.perform(post("/api/v1/community/scraps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ScrapCreateRequest(10L, null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.postId").value(10))
                    .andExpect(jsonPath("$.data.folderId").value(3))
                    .andExpect(jsonPath("$.data.scrapped").value(true));
        }

        @Test
        @DisplayName("postId가 없으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenPostIdMissing() throws Exception {
            authenticateAs(appUser(1L));

            mockMvc.perform(post("/api/v1/community/scraps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(scrapService);
        }

        @Test
        @DisplayName("기본 폴더가 없어 서비스가 폴더 없음 예외를 내면 404를 반환한다")
        void returnsNotFoundWhenFolderMissing() throws Exception {
            authenticateAs(appUser(1L));
            willThrow(new BusinessException(ErrorCode.FOLDER_NOT_FOUND))
                    .given(scrapService).scrap(any(), any());

            mockMvc.perform(post("/api/v1/community/scraps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ScrapCreateRequest(10L, null))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("FOLDER-001"));
        }

        @Test
        @DisplayName("이미 스크랩한 게시글이면 409를 반환한다")
        void returnsConflictWhenAlreadyScrapped() throws Exception {
            authenticateAs(appUser(1L));
            willThrow(new BusinessException(ErrorCode.SCRAP_DUPLICATE))
                    .given(scrapService).scrap(any(), any());

            mockMvc.perform(post("/api/v1/community/scraps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ScrapCreateRequest(10L, null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SCRAP-001"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/community/scraps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ScrapCreateRequest(10L, null))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(scrapService);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/community/scraps/{postId}")
    class Unscrap {

        @Test
        @DisplayName("정상 요청이면 스크랩 취소 결과를 반환한다")
        void unscrapsPost() throws Exception {
            authenticateAs(appUser(1L));
            given(scrapService.unscrap(any(), eq(10L))).willReturn(
                    ScrapResponse.builder().postId(10L).scrapped(false).build());

            mockMvc.perform(delete("/api/v1/community/scraps/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.postId").value(10))
                    .andExpect(jsonPath("$.data.scrapped").value(false));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/community/scraps/{postId}/folder/{folderId}")
    class MoveFolder {

        @Test
        @DisplayName("정상 요청이면 이동된 폴더 정보를 반환한다")
        void movesFolder() throws Exception {
            authenticateAs(appUser(1L));
            given(scrapService.moveFolder(any(), eq(10L), eq(4L))).willReturn(
                    ScrapResponse.builder().postId(10L).folderId(4L).scrapped(true).build());

            mockMvc.perform(patch("/api/v1/community/scraps/10/folder/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.folderId").value(4));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/community/folders")
    class GetFolders {

        @Test
        @DisplayName("인증된 유저면 폴더 목록을 반환한다")
        void returnsFolders() throws Exception {
            authenticateAs(appUser(1L));
            given(scrapService.getFolders(any())).willReturn(List.of(
                    FolderResponse.builder().folderId(3L).name("기본 폴더").isDefault(true).build()));

            mockMvc.perform(get("/api/v1/community/folders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].folderId").value(3))
                    .andExpect(jsonPath("$.data[0].isDefault").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/community/folders/{folderId}/scraps")
    class GetScraps {

        @Test
        @DisplayName("폴더별 스크랩 목록을 반환한다")
        void returnsScrapsInFolder() throws Exception {
            authenticateAs(appUser(1L));
            given(scrapService.getScrapsInFolder(any(), eq(3L), eq(0), eq(20))).willReturn(
                    PageResponse.<PostSummaryResponse>builder()
                            .content(List.of(PostSummaryResponse.builder()
                                    .postId(10L)
                                    .imageUrls(List.of("https://img/a.jpg"))
                                    .scrappedByMe(true)
                                    .build()))
                            .page(0)
                            .size(20)
                            .totalElements(1)
                            .totalPages(1)
                            .hasNext(false)
                            .build());

            mockMvc.perform(get("/api/v1/community/folders/3/scraps"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].postId").value(10))
                    .andExpect(jsonPath("$.data.content[0].imageUrls[0]").value("https://img/a.jpg"));
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
