package com.maggu.maggu.mypage.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.mypage.dto.LocaleUpdateRequest;
import com.maggu.maggu.mypage.dto.MyAccountResponse;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyPage", description = "마이페이지 관련 API")
@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class MyPageController {
    private final UserService userService;

    @Operation(summary = "내 계정 정보 조회", description = "로그인한 본인의 provider/email/nickname/locale을 조회한다.")
    @GetMapping("/account")
    public MyAccountResponse getMyAccount(@CurrentUser AppUser user) {
        return userService.getMyAccount(user);
    }

    @Operation(summary = "앱 언어 변경", description = "지도 TourAPI 언어 기본값으로 사용한다. 요청마다 lang을 보내면 그 값이 우선한다.")
    @PatchMapping("/locale")
    public MyAccountResponse updateLocale(
            @CurrentUser AppUser user,
            @Valid @RequestBody LocaleUpdateRequest request
    ) {
        return userService.updateLocale(user, request.locale());
    }
}
