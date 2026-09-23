package com.maggu.maggu.auth.controller;

import com.maggu.maggu.auth.dto.AppleLoginReq;
import com.maggu.maggu.auth.dto.TokenResponse;
import com.maggu.maggu.auth.dto.WithdrawResponse;
import com.maggu.maggu.auth.service.AuthService;
import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "애플로그인하기",
            description = "Apple identity token을 검증하고 서비스 JWT를 발급한다. 신규 가입 시 locale(KO/EN)을 함께 보내면 저장되고, 없으면 KO.")
    @PostMapping("/login")
    public TokenResponse loginWithApple(@Valid @RequestBody AppleLoginReq request) {
        return authService.loginWithApple(request);
    }

    @Operation(summary = "회원 탈퇴하기", description = "Apple 연동을 해제(Revoke)하고 회원 정보를 삭제한다.")
    @DeleteMapping("/delete")
    public WithdrawResponse withdraw(@Parameter(hidden = true) @CurrentUser AppUser user) {
        return authService.withdraw(user);
    }

}
