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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

    @Nested
    @DisplayName("validateObjectKey")
    class ValidateObjectKey {

        private static final String UUID_VALUE = "2d97184d-60ab-4b5a-9cb8-ac56c969320f";

        @Test
        @DisplayName("본인이 발급받은 {DOMAIN}/{userId}/{uuid}.{ext} 형식이면 통과한다")
        void passesWhenKeyMatchesIssuedFormat() {
            AppUser user = appUser(12L);

            assertThatCode(() -> presignedUrlService.validateObjectKey(
                    user, UploadDomain.STICKER, "STICKER/12/" + UUID_VALUE + ".png"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("generatePresignedUrl이 발급한 objectKey는 그대로 검증을 통과한다")
        void passesForKeyIssuedByGeneratePresignedUrl() throws Exception {
            AppUser user = appUser(1L);
            given(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                    .willReturn(new URL("https://test-bucket.s3.amazonaws.com/signed"));
            String objectKey = presignedUrlService
                    .generatePresignedUrl(user, new PresignedUrlRequest("image/heic", "STICKER"))
                    .objectKey();

            assertThatCode(() -> presignedUrlService.validateObjectKey(user, UploadDomain.STICKER, objectKey))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
                "string",
                "https://test-bucket.s3.ap-northeast-2.amazonaws.com/STICKER/12/" + UUID_VALUE + ".png?X-Amz-Signature=abc",
                "STICKER/99/" + UUID_VALUE + ".png",
                "POST/12/" + UUID_VALUE + ".png",
                "STICKER/12/" + UUID_VALUE + ".exe",
                "STICKER/12/2D97184D-60AB-4B5A-9CB8-AC56C969320F.png",
                "STICKER/12/x/" + UUID_VALUE + ".png",
                "STICKER/12/" + UUID_VALUE,
                "/STICKER/12/" + UUID_VALUE + ".png"
        })
        @DisplayName("발급 형식이 아니거나 다른 유저/도메인의 키면 UPLOAD_INVALID_OBJECT_KEY 예외를 던진다")
        void throwsWhenKeyInvalid(String objectKey) {
            AppUser user = appUser(12L);

            assertThatThrownBy(() -> presignedUrlService.validateObjectKey(user, UploadDomain.STICKER, objectKey))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_OBJECT_KEY));
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
