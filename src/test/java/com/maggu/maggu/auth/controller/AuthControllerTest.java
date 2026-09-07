package com.maggu.maggu.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.auth.dto.AppleLoginReq;
import com.maggu.maggu.auth.dto.TokenResponse;
import com.maggu.maggu.auth.dto.WithdrawResponse;
import com.maggu.maggu.auth.service.AuthService;
import com.maggu.maggu.global.entity.enums.Provider;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/apple")
    class LoginWithApple {

        @Test
        @DisplayName("유효한 요청이면 200과 함께 서비스 JWT를 반환한다")
        void returnsTokens() throws Exception {
            given(authService.loginWithApple(any()))
                    .willReturn(TokenResponse.of("access-token", "refresh-token", 3600L));

            mockMvc.perform(post("/api/v1/auth/apple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AppleLoginReq("identity-token", "auth-code", "윤시진"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(3600));
        }

        @Test
        @DisplayName("identityToken이 없으면 400을 반환하고 서비스를 호출하지 않는다")
        void rejectsBlankIdentityToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/apple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"identityToken":"","authorizationCode":null,"fullName":"윤시진"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(authService);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/auth/withdraw")
    class Withdraw {

        @Test
        @DisplayName("인증된 유저면 200과 함께 탈퇴 결과를 반환한다")
        void withdrawsAuthenticatedUser() throws Exception {
            AppUser user = appUser(1L);
            authenticateAs(user);
            given(authService.withdraw(any())).willReturn(WithdrawResponse.ok());

            mockMvc.perform(delete("/api/v1/auth/withdraw"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.withdrawn").value(true));

            verify(authService).withdraw(any());
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스를 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(delete("/api/v1/auth/withdraw"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(authService);
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
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
