package com.maggu.maggu.user.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.user.dto.request.LocaleUpdateRequest;
import com.maggu.maggu.user.dto.request.ProfileUpdateRequest;
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

    @Operation(summary = "프로필 변경",
            description = "프로필 수정 화면의 저장 버튼용 API (닉네임과 프로필 이미지를 한 번에 저장) / " +
                    "nickname: 앞뒤 공백을 제거한 뒤 30자 이하 & 다른 사용자가 쓰는 닉네임이면 409(USER-001) / " +
                    "imageKey: 새 이미지는 POST /api/v1/uploads/presigned-url(domain=PROFILE)로 업로드한 objectKey, " +
                    "이미지를 바꾸지 않으면 현재 키를 그대로 기본 이미지로 되돌리려면 null / " +
                    "본인 PROFILE 키가 아니면 400(UPLOAD-002) 검증에 하나라도 실패하면 닉네임/이미지 모두 변경되지 않음")
    @PutMapping("/me/profile")
    public MyAccountResponse updateProfile(@CurrentUser AppUser user,
                                           @Valid @RequestBody ProfileUpdateRequest request) {
        return userService.updateProfile(user, request);
    }
}
