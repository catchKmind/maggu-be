package com.maggu.maggu.mypage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.security.CustomUserDetails;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
import com.maggu.maggu.mypage.dto.LocaleUpdateRequest;
import com.maggu.maggu.mypage.dto.MyAccountResponse;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.service.UserService;
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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MyPageController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/mypage/account")
    class GetMyAccount {

        @Test
        @DisplayName("인증된 유저면 provider/email/nickname/locale을 반환한다")
        void returnsAccount() throws Exception {
            AppUser user = appUser(1L);
            authenticateAs(user);
            given(userService.getMyAccount(user))
                    .willReturn(MyAccountResponse.from(user));

            mockMvc.perform(get("/api/v1/mypage/account"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.provider").value("APPLE"))
                    .andExpect(jsonPath("$.data.email").value("user@test.com"))
                    .andExpect(jsonPath("$.data.nickname").value("시진"))
                    .andExpect(jsonPath("$.data.locale").value("KO"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환한다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/mypage/account"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/mypage/locale")
    class UpdateLocale {

        @Test
        @DisplayName("인증된 유저면 locale을 변경한다")
        void updatesLocale() throws Exception {
            AppUser user = appUser(1L);
            authenticateAs(user);
            given(userService.updateLocale(user, AppLocale.EN))
                    .willReturn(new MyAccountResponse(Provider.APPLE, "user@test.com", "시진", AppLocale.EN));

            mockMvc.perform(patch("/api/v1/mypage/locale")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LocaleUpdateRequest(AppLocale.EN))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.locale").value("EN"));

            verify(userService).updateLocale(user, AppLocale.EN);
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
                .email("user@test.com")
                .nickname("시진")
                .locale(AppLocale.KO)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
