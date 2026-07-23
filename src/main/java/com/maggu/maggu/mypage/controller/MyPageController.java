package com.maggu.maggu.mypage.controller;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.mypage.dto.MyAccountResponse;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyPage", description = "마이페이지 관련 API")
@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {
    private final UserService userService;

    @Operation(summary = "내 계정 정보 조회", description = "로그인한 본인의 provider/email/nickname을 조회한다.")
    @GetMapping("/account")
    public MyAccountResponse getMyAccount(@CurrentUser AppUser user) {
        return userService.getMyAccount(user);
    }
}
