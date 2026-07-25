package com.maggu.maggu.auth.controller;

import com.maggu.maggu.auth.dto.TestLoginRequest;
import com.maggu.maggu.auth.dto.TokenResponse;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.security.jwt.JwtTokenProvider;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Profile({"local", "dev", "default"})
public class AuthController {

    private final UserRepository appUserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/test-login")
    public TokenResponse testLogin(@Valid @RequestBody TestLoginRequest request) {
        AppUser appUser = appUserRepository
                .findByProviderAndProviderUserId(Provider.TEST, request.email())
                .orElseGet(() -> createTestUser(request));

        String accessToken = jwtTokenProvider.createAccessToken(appUser.getId(), appUser.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(appUser.getId());

        return TokenResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessTokenValiditySeconds());
    }

    private AppUser createTestUser(TestLoginRequest request) {
        if (appUserRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }

        return appUserRepository.save(
                AppUser.builder()
                        .provider(Provider.TEST)
                        .providerUserId(request.email())
                        .email(request.email())
                        .nickname(request.nickname())
                        .build()
        );
    }
}