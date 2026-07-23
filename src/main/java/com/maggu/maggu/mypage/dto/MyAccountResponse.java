package com.maggu.maggu.mypage.dto;

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

    public static MyAccountResponse from(AppUser user) {
        return new MyAccountResponse(
                user.getProvider(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
