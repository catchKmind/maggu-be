package com.maggu.maggu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AppleLoginReq(
        @NotBlank
        @Schema(description = "Apple identity token (JWT)", example = "eyJraWQiOiJlWGF1bm1...")
        String identityToken,

        @Schema(description = "Apple authorization code. 서버 간 토큰 교환이 필요할 때 사용한다.")
        String authorizationCode,

        @Schema(description = "사용자 이름. Apple은 최초 로그인 시에만 제공한다.", example = "윤시진")
        String fullName
) {
}
