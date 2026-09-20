package com.maggu.maggu.global.storage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PresignedUrlServiceTest {

    @Mock
    private AmazonS3 amazonS3;

    @InjectMocks
    private PresignedUrlService presignedUrlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(presignedUrlService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(presignedUrlService, "expirationSeconds", 300L);
    }

    @Nested
    @DisplayName("generatePresignedUrl")
    class GeneratePresignedUrl {

        @Test
        @DisplayName("허용된 contentType이면 presignedUrl과 objectKey를 발급한다")
        void issuesPresignedUrl() throws Exception {
            AppUser user = appUser(1L);
            PresignedUrlRequest request = new PresignedUrlRequest("image/png", "STICKER");
            URL signedUrl = new URL("https://test-bucket.s3.amazonaws.com/signed");
            given(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).willReturn(signedUrl);

            PresignedUrlResponse result = presignedUrlService.generatePresignedUrl(user, request);

            assertThat(result.presignedUrl()).isEqualTo(signedUrl.toString());
            assertThat(result.objectKey()).matches("STICKER/1/[0-9a-f-]{36}\\.png");
        }

        @Test
        @DisplayName("버킷/키/HTTP 메서드/Content-Type을 정확히 담아 S3에 서명을 요청한다")
        void buildsGeneratePresignedUrlRequestCorrectly() throws Exception {
            AppUser user = appUser(1L);
            PresignedUrlRequest request = new PresignedUrlRequest("image/webp", "POST");
            ArgumentCaptor<GeneratePresignedUrlRequest> captor = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
            given(amazonS3.generatePresignedUrl(captor.capture()))
                    .willReturn(new URL("https://test-bucket.s3.amazonaws.com/signed"));

            PresignedUrlResponse result = presignedUrlService.generatePresignedUrl(user, request);

            GeneratePresignedUrlRequest captured = captor.getValue();
            assertThat(captured.getBucketName()).isEqualTo("test-bucket");
            assertThat(captured.getKey()).isEqualTo(result.objectKey());
            assertThat(captured.getMethod()).isEqualTo(HttpMethod.PUT);
            assertThat(captured.getContentType()).isEqualTo("image/webp");
        }

        @Test
        @DisplayName("허용되지 않은 contentType이면 예외를 던지고 S3를 호출하지 않는다")
        void throwsWhenContentTypeNotAllowed() {
            AppUser user = appUser(1L);
            PresignedUrlRequest request = new PresignedUrlRequest("application/pdf", "STICKER");

            assertThatThrownBy(() -> presignedUrlService.generatePresignedUrl(user, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_UNSUPPORTED_CONTENT_TYPE));

            verifyNoInteractions(amazonS3);
        }

        @Test
        @DisplayName("domain 값이 STICKER/POST가 아니면 예외를 던지고 S3를 호출하지 않는다")
        void throwsWhenDomainInvalid() {
            AppUser user = appUser(1L);
            PresignedUrlRequest request = new PresignedUrlRequest("image/png", "INVALID");

            assertThatThrownBy(() -> presignedUrlService.generatePresignedUrl(user, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(amazonS3);
        }
    }

    private AppUser appUser(Long id) {
        AppUser user = AppUser.builder()
                .provider(Provider.GOOGLE)
                .providerUserId("google-uid")
                .email("test@test.com")
                .nickname("나그네")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
