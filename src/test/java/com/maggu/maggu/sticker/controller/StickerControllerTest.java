package com.maggu.maggu.sticker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.security.CustomUserDetails;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
import com.maggu.maggu.sticker.dto.StickerCreateRequest;
import com.maggu.maggu.sticker.dto.StickerResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StickerController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class StickerControllerTest {

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
                    StickerResponse.builder().stickerId(10L).imageUrl("https://img/new.png").build());

            mockMvc.perform(post("/api/v1/stickers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StickerCreateRequest("https://img/new.png"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.stickerId").value(10))
                    .andExpect(jsonPath("$.data.imageUrl").value("https://img/new.png"));
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
                            .content(objectMapper.writeValueAsString(new StickerCreateRequest("https://img/new.png"))))
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
