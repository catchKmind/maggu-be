package com.maggu.maggu.auth.apple;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleAuthService {

    private final AppleAuthClient appleAuthClient;

    public void syncRefreshToken(AppUser user, String authorizationCode) {
        if (user == null || user.getProvider() != Provider.APPLE || !StringUtils.hasText(authorizationCode)) {
            return;
        }
        String refreshToken = appleAuthClient.exchangeAuthorizationCode(authorizationCode);
        user.updateAppleRefreshToken(refreshToken);
    }

    public void revoke(AppUser user) {
        if (user == null || user.getProvider() != Provider.APPLE) {
            return;
        }
        String refreshToken = user.getAppleRefreshToken();
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("Apple refresh token이 없어 revoke를 건너뜁니다. userId={}", user.getId());
            return;
        }
        appleAuthClient.revokeRefreshToken(refreshToken);
    }
}
