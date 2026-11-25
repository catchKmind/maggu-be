package com.maggu.maggu.user.dto.response;

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MyAccountResponse {

    @Schema(description = "소셜 로그인 제공자", example = "GOOGLE")
    private Provider provider;

    @Schema(description = "이메일", example = "test@gmail.com")
    private String email;

    @Schema(description = "닉네임", example = "행복한사자123")
    private String nickname;

    @Schema(description = "프로필 이미지 URL, 없으면 null", example = "https://example.com/PROFILE/1/uuid.jpg")
    private String profileImageUrl;

    @Schema(description = "프로필 이미지 objectKey, 없으면 null / " +
            "프로필 수정(PUT /api/v1/users/me/profile) 시 이미지를 바꾸지 않으면 이 값을 imageKey로 그대로 보낸다.",
            example = "PROFILE/1/3f2a8c1e-9b7d-4e2a-8f1c-2d3e4f5a6b7c.png")
    private String profileImageKey;

    @Schema(description = "앱 언어", example = "KO")
    private AppLocale locale;

    public static MyAccountResponse from(AppUser user, String profileImageUrl) {
        return new MyAccountResponse(
                user.getProvider(),
                user.getEmail(),
                user.getNickname(),
                profileImageUrl,
                user.getProfileImageUrl(), // 컬럼명은 _url이지만 실제 값은 objectKey
                user.getLocale()
        );
    }
}
