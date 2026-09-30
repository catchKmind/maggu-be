package com.maggu.maggu.global.storage;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectKeyValidatorTest {

    private static final String STICKER_KEY = "STICKER/1/550e8400-e29b-41d4-a716-446655440000.png";
    private static final String POST_KEY = "POST/1/550e8400-e29b-41d4-a716-446655440000.jpeg";

    private ObjectKeyValidator validator;
    private AppUser user;

    @BeforeEach
    void setUp() {
        validator = new ObjectKeyValidator();
        user = AppUser.builder()
                .provider(Provider.APPLE)
                .providerUserId("apple-1")
                .email("user@test.com")
                .nickname("유저")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("유효한 STICKER objectKey를 그대로 반환한다")
    void acceptsValidStickerKey() {
        assertThat(validator.validateStickerObjectKey(user, STICKER_KEY)).isEqualTo(STICKER_KEY);
    }

    @Test
    @DisplayName("앞에 슬래시가 있어도 정규화해 허용한다")
    void trimsLeadingSlash() {
        assertThat(validator.validateStickerObjectKey(user, "/" + STICKER_KEY)).isEqualTo(STICKER_KEY);
    }

    @Test
    @DisplayName("다른 유저의 objectKey는 거부한다")
    void rejectsOtherUsersKey() {
        assertThatThrownBy(() -> validator.validateStickerObjectKey(user, "STICKER/2/550e8400-e29b-41d4-a716-446655440000.png"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_OBJECT_KEY));
    }

    @Test
    @DisplayName("HTTP URL은 거부한다")
    void rejectsHttpUrl() {
        assertThatThrownBy(() -> validator.validateStickerObjectKey(user, "https://bucket.s3.amazonaws.com/" + STICKER_KEY))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_OBJECT_KEY));
    }

    @Test
    @DisplayName("빈 POST imageUrls 원소는 거부한다")
    void rejectsBlankPostImageEntry() {
        assertThatThrownBy(() -> validator.validatePostObjectKeys(user, List.of(POST_KEY, " ")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_OBJECT_KEY));
    }

    @Test
    @DisplayName("null 또는 빈 POST imageUrls 목록은 빈 리스트를 반환한다")
    void acceptsEmptyPostImageList() {
        assertThat(validator.validatePostObjectKeys(user, null)).isEmpty();
        assertThat(validator.validatePostObjectKeys(user, List.of())).isEmpty();
    }
}
