package com.maggu.maggu.global.i18n;

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.security.CustomUserDetails;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLocaleResolverTest {

    private final RequestLocaleResolver resolver = new RequestLocaleResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("query lang이 있으면 유저 locale보다 우선한다")
    void queryLangOverridesUserLocale() {
        authenticateAs(AppLocale.KO);

        assertThat(resolver.resolve("EN")).isEqualTo(AppLocale.EN);
    }

    @Test
    @DisplayName("query lang이 없으면 로그인한 유저의 locale을 쓴다")
    void fallsBackToUserLocale() {
        authenticateAs(AppLocale.EN);

        assertThat(resolver.resolve(null)).isEqualTo(AppLocale.EN);
    }

    @Test
    @DisplayName("lang도 유저도 없으면 KO다")
    void defaultsToKo() {
        assertThat(resolver.resolve(null)).isEqualTo(AppLocale.KO);
        assertThat(resolver.resolve("")).isEqualTo(AppLocale.KO);
    }

    private void authenticateAs(AppLocale locale) {
        AppUser user = AppUser.builder()
                .provider(Provider.APPLE)
                .providerUserId("apple-uid")
                .email("user@test.com")
                .nickname("시진")
                .locale(locale)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, List.of()));
    }
}
