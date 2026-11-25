package com.maggu.maggu.user.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.user.dto.request.LocaleUpdateRequest;
import com.maggu.maggu.user.dto.request.ProfileImageUpdateRequest;
import com.maggu.maggu.user.dto.response.MyAccountResponse;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyAccount", description = "내 계정 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class MyAccountController {

    private final UserService userService;

    @Operation(summary = "내 계정 정보 조회", description = "로그인한 본인의 provider/email/nickname/profileImageUrl/locale을 조회한다.")
    @GetMapping("/me")
    public MyAccountResponse getMyAccount(@CurrentUser AppUser user) {
        return userService.getMyAccount(user);
    }

    @Operation(summary = "앱 언어 변경", description = "지도 TourAPI 언어 기본값으로 사용한다. 요청마다 lang을 보내면 그 값이 우선한다.")
    @PatchMapping("/me/locale")
    public MyAccountResponse updateLocale(
            @CurrentUser AppUser user,
            @Valid @RequestBody LocaleUpdateRequest request
    ) {
        return userService.updateLocale(user, request.locale());
    }

    @Operation(summary = "프로필 이미지 변경",
            description = "POST /api/v1/uploads/presigned-url(domain=PROFILE)로 발급받은 URL에 업로드한 뒤, 응답의 objectKey를 imageKey로 보낸다. " +
                    "본인 PROFILE 키가 아니면 400(UPLOAD-002) / 기존과 같은 키면 변경 없이 현재 계정 정보를 반환한다.")
    @PatchMapping("/me/profile-image")
    public MyAccountResponse updateProfileImage(@CurrentUser AppUser user,
                                                @Valid @RequestBody ProfileImageUpdateRequest request) {
        return userService.updateProfileImage(user, request);
    }

    @Operation(summary = "프로필 이미지 삭제(기본 이미지로 변경)",
            description = "프로필 이미지를 제거한다. " +
                    "응답의 profileImageUrl은 null이며, FE가 기본 이미지를 표시한다. " +
                    "이미 없는 상태여도 200을 반환한다.")
    @DeleteMapping("/me/profile-image")
    public MyAccountResponse clearProfileImage(@CurrentUser AppUser user) {
        return userService.clearProfileImage(user);
    }
}
