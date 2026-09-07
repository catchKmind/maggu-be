package com.maggu.maggu.auth.service;

import com.maggu.maggu.auth.apple.AppleAuthService;
import com.maggu.maggu.auth.apple.AppleJwtValidator;
import com.maggu.maggu.auth.dto.AppleLoginReq;
import com.maggu.maggu.auth.dto.TokenResponse;
import com.maggu.maggu.auth.dto.WithdrawResponse;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.security.jwt.JwtTokenProvider;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.repository.UserRepository;
import com.maggu.maggu.user.service.UserService;
import com.maggu.maggu.user.service.UserWithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AppleJwtValidator appleJwtValidator;
    private final AppleAuthService appleAuthService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserWithdrawalService userWithdrawalService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse loginWithApple(AppleLoginReq request) {
        Map<String, Object> claims = appleJwtValidator.validate(request.identityToken());
        String appleUserId = appleJwtValidator.getSubject(claims);
        if (!StringUtils.hasText(appleUserId)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        AppUser user = userRepository.findByProviderAndProviderUserId(Provider.APPLE, appleUserId)
                .orElseGet(() -> registerAppleUser(appleUserId, appleJwtValidator.getEmail(claims), request.fullName()));

        syncAppleRefreshToken(user, request.authorizationCode());

        return issueTokens(user);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public WithdrawResponse withdraw(AppUser currentUser) {
        AppUser user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (user.getProvider() == Provider.APPLE) {
            appleAuthService.revoke(user);
        }
        userWithdrawalService.deleteAccount(user);
        return WithdrawResponse.ok();
    }

    private void syncAppleRefreshToken(AppUser user, String authorizationCode) {
        if (!StringUtils.hasText(authorizationCode)) {
            return;
        }
        try {
            appleAuthService.syncRefreshToken(user, authorizationCode);
        } catch (RuntimeException e) {
            log.warn("Apple authorization code 교환 실패. 로그인은 유지합니다. userId={}", user.getId(), e);
        }
    }

    private AppUser registerAppleUser(String appleUserId, String email, String fullName) {
        String resolvedEmail = StringUtils.hasText(email)
                ? email
                : appleUserId + "@privaterelay.appleid.com";

        try {
            AppUser created = userService.createUser(Provider.APPLE, appleUserId, resolvedEmail, fullName);
            userRepository.flush();
            return created;
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByProviderAndProviderUserId(Provider.APPLE, appleUserId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    private TokenResponse issueTokens(AppUser user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        return TokenResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessTokenValiditySeconds());
    }
}
