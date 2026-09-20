package com.maggu.maggu.global.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PresignedUrlController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class PresignedUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PresignedUrlService presignedUrlService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/v1/uploads/presigned-url")
    class GeneratePresignedUrl {

        @Test
        @DisplayName("정상 요청이면 presignedUrl과 objectKey를 반환한다")
        void returnsPresignedUrl() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(presignedUrlService.generatePresignedUrl(any(), any())).willReturn(
                    PresignedUrlResponse.builder()
                            .presignedUrl("https://test-bucket.s3.amazonaws.com/signed")
                            .objectKey("STICKER/1/uuid.png")
                            .build());

            mockMvc.perform(post("/api/v1/uploads/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PresignedUrlRequest("image/png", "STICKER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.presignedUrl").value("https://test-bucket.s3.amazonaws.com/signed"))
                    .andExpect(jsonPath("$.data.objectKey").value("STICKER/1/uuid.png"));
        }

        @Test
        @DisplayName("contentType이 비어 있으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenContentTypeBlank() throws Exception {
            authenticateAs(appUser(1L, "나그네"));

            mockMvc.perform(post("/api/v1/uploads/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PresignedUrlRequest("", "STICKER"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(presignedUrlService);
        }

        @Test
        @DisplayName("domain이 비어 있으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenDomainBlank() throws Exception {
            authenticateAs(appUser(1L, "나그네"));

            mockMvc.perform(post("/api/v1/uploads/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PresignedUrlRequest("image/png", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(presignedUrlService);
        }

        @Test
        @DisplayName("서비스가 지원하지 않는 contentType 예외를 던지면 415를 반환한다")
        void returnsUnsupportedMediaTypeWhenServiceRejectsContentType() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.UPLOAD_UNSUPPORTED_CONTENT_TYPE))
                    .given(presignedUrlService).generatePresignedUrl(any(), any());

            mockMvc.perform(post("/api/v1/uploads/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PresignedUrlRequest("application/pdf", "STICKER"))))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("UPLOAD-001"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/uploads/presigned-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PresignedUrlRequest("image/png", "STICKER"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(presignedUrlService);
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
