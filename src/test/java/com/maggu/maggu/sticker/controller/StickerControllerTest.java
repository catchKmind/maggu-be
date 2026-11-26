package com.maggu.maggu.sticker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.security.CustomUserDetails;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
import com.maggu.maggu.sticker.dto.request.StickerCreateRequest;
import com.maggu.maggu.sticker.dto.request.StickersDeleteRequest;
import com.maggu.maggu.sticker.dto.response.StickerDeleteResponse;
import com.maggu.maggu.sticker.dto.response.StickerResponse;
import com.maggu.maggu.sticker.dto.response.StickersDeleteResponse;
import com.maggu.maggu.sticker.service.StickerService;
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
import java.util.stream.LongStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StickerController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class StickerControllerTest {

    private static final String STICKER_OBJECT_KEY = "STICKER/1/550e8400-e29b-41d4-a716-446655440000.png";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StickerService stickerService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/stickers")
    class GetMyStickers {

        @Test
        @DisplayName("인증된 유저면 200과 함께 커스텀 스티커 목록을 반환한다")
        void returnsMyStickers() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(stickerService.getMyStickers(any())).willReturn(List.of(
                    StickerResponse.builder().stickerId(1L).imageUrl("https://img/1.png").build()));

            mockMvc.perform(get("/api/v1/stickers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].stickerId").value(1))
                    .andExpect(jsonPath("$.data[0].imageUrl").value("https://img/1.png"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/stickers"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(stickerService);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/stickers")
    class CreateMySticker {

        @Test
        @DisplayName("정상 요청이면 생성된 스티커 정보를 반환한다")
        void createsSticker() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(stickerService.createMySticker(any(), any())).willReturn(
                    StickerResponse.builder().stickerId(10L).imageUrl(STICKER_OBJECT_KEY).build());

            mockMvc.perform(post("/api/v1/stickers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StickerCreateRequest(STICKER_OBJECT_KEY))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.stickerId").value(10))
                    .andExpect(jsonPath("$.data.imageUrl").value(STICKER_OBJECT_KEY));
        }

        @Test
        @DisplayName("imageUrl이 비어 있으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenImageUrlBlank() throws Exception {
            authenticateAs(appUser(1L, "나그네"));

            mockMvc.perform(post("/api/v1/stickers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StickerCreateRequest(""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(stickerService);
        }

        @Test
        @DisplayName("imageUrl이 500자를 넘으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenImageUrlTooLong() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            String tooLongUrl = "https://img/" + "a".repeat(500);

            mockMvc.perform(post("/api/v1/stickers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StickerCreateRequest(tooLongUrl))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(stickerService);
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/stickers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StickerCreateRequest(STICKER_OBJECT_KEY))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(stickerService);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/stickers/{stickerId}")
    class DeleteMySticker {

        @Test
        @DisplayName("본인 소유 스티커면 200과 함께 삭제 결과를 반환한다")
        void deletesSticker() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(stickerService.deleteMySticker(any(), eq(10L))).willReturn(
                    StickerDeleteResponse.builder().stickerId(10L).deleted(true).build());

            mockMvc.perform(delete("/api/v1/stickers/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.stickerId").value(10))
                    .andExpect(jsonPath("$.data.deleted").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 스티커면 404를 반환한다")
        void returnsNotFoundWhenStickerMissing() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.STICKER_NOT_FOUND))
                    .given(stickerService).deleteMySticker(any(), anyLong());

            mockMvc.perform(delete("/api/v1/stickers/10"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("STICKER-001"));
        }

        @Test
        @DisplayName("다른 유저 소유 스티커면 403을 반환한다")
        void returnsForbiddenWhenNotOwner() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.STICKER_ACCESS_DENIED))
                    .given(stickerService).deleteMySticker(any(), anyLong());

            mockMvc.perform(delete("/api/v1/stickers/10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("STICKER-002"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(delete("/api/v1/stickers/10"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(stickerService);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/stickers/bulk-delete")
    class DeleteMyStickers {

        @Test
        @DisplayName("정상 요청이면 200과 함께 요청한 스티커 ID 목록을 반환한다")
        void deletesStickers() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(stickerService.deleteMyStickers(any(), any())).willReturn(StickersDeleteResponse.from(List.of(10L, 11L)));

            mockMvc.perform(post("/api/v1/stickers/bulk-delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StickersDeleteRequest(List.of(10L, 11L)))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.stickerIds[0]").value(10))
                    .andExpect(jsonPath("$.data.stickerIds[1]").value(11));
        }

        @Test
        @DisplayName("다른 유저 소유 스티커가 섞여 있으면 403을 반환한다")
        void returnsForbiddenWhenNotOwner() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.STICKER_ACCESS_DENIED))
                    .given(stickerService).deleteMyStickers(any(), any());

            mockMvc.perform(post("/api/v1/stickers/bulk-delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StickersDeleteRequest(List.of(10L, 11L)))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("STICKER-002"));
        }

        @Test
        @DisplayName("stickerIds가 비어 있으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenEmpty() throws Exception {
            authenticateAs(appUser(1L, "나그네"));

            mockMvc.perform(post("/api/v1/stickers/bulk-delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stickerIds\": []}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(stickerService);
        }

        @Test
        @DisplayName("stickerIds 필드가 없으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenMissing() throws Exception {
            authenticateAs(appUser(1L, "나그네"));

            mockMvc.perform(post("/api/v1/stickers/bulk-delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(stickerService);
        }

        @Test
        @DisplayName("stickerIds가 20개를 넘으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenTooMany() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            List<Long> ids = LongStream.rangeClosed(1, 21).boxed().toList();

            mockMvc.perform(post("/api/v1/stickers/bulk-delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StickersDeleteRequest(ids))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(stickerService);
        }

        @Test
        @DisplayName("stickerIds가 정확히 20개면 정상 처리한다")
        void acceptsExactlyMaxSize() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            List<Long> ids = LongStream.rangeClosed(1, 20).boxed().toList();
            given(stickerService.deleteMyStickers(any(), any())).willReturn(StickersDeleteResponse.from(ids));

            mockMvc.perform(post("/api/v1/stickers/bulk-delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StickersDeleteRequest(ids))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("stickerIds에 null 원소가 있으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenNullElement() throws Exception {
            authenticateAs(appUser(1L, "나그네"));

            mockMvc.perform(post("/api/v1/stickers/bulk-delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stickerIds\": [10, null]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(stickerService);
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/stickers/bulk-delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StickersDeleteRequest(List.of(10L)))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(stickerService);
        }
    }

    private void authenticateAs(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, List.of()));
    }

    private AppUser appUser(Long id, String nickname) {
        AppUser user = AppUser.builder()
                .provider(Provider.GOOGLE)
                .providerUserId("google-uid")
                .email("test@test.com")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
