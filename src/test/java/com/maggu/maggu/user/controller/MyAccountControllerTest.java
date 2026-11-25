package com.maggu.maggu.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.security.CustomUserDetails;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.user.dto.request.LocaleUpdateRequest;
import com.maggu.maggu.user.dto.request.ProfileUpdateRequest;
import com.maggu.maggu.user.dto.response.MyAccountResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MyAccountController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class MyAccountControllerTest {

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
    @DisplayName("GET /api/v1/users/me")
    class GetMyAccount {

        @Test
        @DisplayName("인증된 유저면 provider/email/nickname/profileImageUrl/locale을 반환한다")
        void returnsAccount() throws Exception {
            AppUser user = appUser(1L);
            ReflectionTestUtils.setField(user, "profileImageUrl", "PROFILE/1/uuid.png");
            authenticateAs(user);
            given(userService.getMyAccount(user))
                    .willReturn(MyAccountResponse.from(user, "https://cdn.example.com/PROFILE/1/uuid.png"));

            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.provider").value("APPLE"))
                    .andExpect(jsonPath("$.data.email").value("user@test.com"))
                    .andExpect(jsonPath("$.data.nickname").value("시진"))
                    .andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.example.com/PROFILE/1/uuid.png"))
                    .andExpect(jsonPath("$.data.profileImageKey").value("PROFILE/1/uuid.png"))
                    .andExpect(jsonPath("$.data.locale").value("KO"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환한다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/me/locale")
    class UpdateLocale {

        @Test
        @DisplayName("인증된 유저면 locale을 변경한다")
        void updatesLocale() throws Exception {
            AppUser user = appUser(1L);
            authenticateAs(user);
            given(userService.updateLocale(user, AppLocale.EN))
                    .willReturn(new MyAccountResponse(Provider.APPLE, "user@test.com", "시진", null, null, AppLocale.EN));

            mockMvc.perform(patch("/api/v1/users/me/locale")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LocaleUpdateRequest(AppLocale.EN))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.locale").value("EN"));

            verify(userService).updateLocale(user, AppLocale.EN);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me/profile")
    class UpdateProfile {

        private static final String URL = "/api/v1/users/me/profile";
        private static final String IMAGE_KEY = "PROFILE/1/3f2a8c1e-9b7d-4e2a-8f1c-2d3e4f5a6b7c.png";

        @Test
        @DisplayName("인증된 유저면 닉네임과 프로필 이미지를 변경하고 변경된 계정 정보를 반환한다")
        void updatesProfile() throws Exception {
            AppUser user = appUser(1L);
            authenticateAs(user);
            ProfileUpdateRequest request = new ProfileUpdateRequest("새닉네임", IMAGE_KEY);
            given(userService.updateProfile(user, request))
                    .willReturn(new MyAccountResponse(Provider.APPLE, "user@test.com", "새닉네임",
                            "https://cdn.example.com/" + IMAGE_KEY, IMAGE_KEY, AppLocale.KO));

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
                    .andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.example.com/" + IMAGE_KEY))
                    .andExpect(jsonPath("$.data.profileImageKey").value(IMAGE_KEY));

            verify(userService).updateProfile(user, request);
        }

        @Test
        @DisplayName("imageKey가 null이면(기본 이미지) 그대로 서비스에 전달한다")
        void passesNullImageKey() throws Exception {
            AppUser user = appUser(1L);
            authenticateAs(user);
            ProfileUpdateRequest request = new ProfileUpdateRequest("새닉네임", null);
            given(userService.updateProfile(user, request))
                    .willReturn(new MyAccountResponse(Provider.APPLE, "user@test.com", "새닉네임", null, null, AppLocale.KO));

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"새닉네임\",\"imageKey\":null}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist())
                    .andExpect(jsonPath("$.data.profileImageKey").doesNotExist());

            verify(userService).updateProfile(user, request);
        }

        @Test
        @DisplayName("닉네임 앞뒤 공백은 제거한 뒤 서비스에 전달한다")
        void trimsNickname() throws Exception {
            AppUser user = appUser(1L);
            authenticateAs(user);

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"  새닉네임  \",\"imageKey\":\"" + IMAGE_KEY + "\"}"))
                    .andExpect(status().isOk());

            verify(userService).updateProfile(user, new ProfileUpdateRequest("새닉네임", IMAGE_KEY));
        }

        @Test
        @DisplayName("공백을 제거하면 30자인 닉네임은 허용한다")
        void allowsThirtyCharsAfterTrim() throws Exception {
            authenticateAs(appUser(1L));
            String thirtyChars = "가".repeat(30);

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"  " + thirtyChars + "  \",\"imageKey\":null}"))
                    .andExpect(status().isOk());

            verify(userService).updateProfile(any(), eq(new ProfileUpdateRequest(thirtyChars, null)));
        }

        @Test
        @DisplayName("닉네임이 30자를 넘으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenNicknameTooLong() throws Exception {
            authenticateAs(appUser(1L));

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"" + "가".repeat(31) + "\",\"imageKey\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("닉네임이 공백뿐이면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenNicknameBlank() throws Exception {
            authenticateAs(appUser(1L));

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"   \",\"imageKey\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("닉네임 필드가 없으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenNicknameMissing() throws Exception {
            authenticateAs(appUser(1L));

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"imageKey\":\"" + IMAGE_KEY + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 409(USER-001)를 반환한다")
        void returnsConflictWhenNicknameDuplicate() throws Exception {
            authenticateAs(appUser(1L));
            willThrow(new BusinessException(ErrorCode.NICKNAME_DUPLICATE))
                    .given(userService).updateProfile(any(), any());

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ProfileUpdateRequest("중복닉네임", IMAGE_KEY))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("USER-001"));
        }

        @Test
        @DisplayName("본인 PROFILE 키가 아니면 400(UPLOAD-002)을 반환한다")
        void returnsBadRequestWhenObjectKeyInvalid() throws Exception {
            authenticateAs(appUser(1L));
            willThrow(new BusinessException(ErrorCode.UPLOAD_INVALID_OBJECT_KEY))
                    .given(userService).updateProfile(any(), any());

            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ProfileUpdateRequest("새닉네임", "STICKER/1/uuid.png"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("UPLOAD-002"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(put(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ProfileUpdateRequest("새닉네임", IMAGE_KEY))))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(userService);
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
