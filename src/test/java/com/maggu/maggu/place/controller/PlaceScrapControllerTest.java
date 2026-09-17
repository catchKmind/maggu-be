package com.maggu.maggu.place.controller;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.security.CustomUserDetails;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
import com.maggu.maggu.place.dto.response.PlaceFolderResponse;
import com.maggu.maggu.place.service.PlaceScrapService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlaceScrapController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class PlaceScrapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceScrapService placeScrapService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/my-places/folders")
    class GetPlaceFolders {

        @Test
        @DisplayName("인증된 유저면 200과 함께 장소 스크랩 폴더 목록을 반환한다")
        void returnsPlaceFolders() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(placeScrapService.getPlaceFolders(any())).willReturn(List.of(
                    PlaceFolderResponse.builder()
                            .placeFolderId(1L)
                            .name("내 장소")
                            .icon("❤️")
                            .isDefault(true)
                            .build(),
                    PlaceFolderResponse.builder()
                            .placeFolderId(2L)
                            .name("가보고 싶은 곳")
                            .icon("✈️")
                            .isDefault(false)
                            .build()));

            mockMvc.perform(get("/api/v1/my-places/folders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].placeFolderId").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("내 장소"))
                    .andExpect(jsonPath("$.data[0].icon").value("❤️"))
                    .andExpect(jsonPath("$.data[0].isDefault").value(true))
                    .andExpect(jsonPath("$.data[1].placeFolderId").value(2))
                    .andExpect(jsonPath("$.data[1].isDefault").value(false));
        }

        @Test
        @DisplayName("폴더가 없으면 빈 배열을 반환한다")
        void returnsEmptyListWhenNoFolders() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(placeScrapService.getPlaceFolders(any())).willReturn(List.of());

            mockMvc.perform(get("/api/v1/my-places/folders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/my-places/folders"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(placeScrapService);
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
