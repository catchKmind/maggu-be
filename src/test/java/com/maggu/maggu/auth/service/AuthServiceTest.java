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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppleJwtValidator appleJwtValidator;

    @Mock
    private AppleAuthService appleAuthService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserWithdrawalService userWithdrawalService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("loginWithApple")
    class LoginWithApple {

        @Test
        @DisplayName("기존 Apple 회원이면 신규 등록 없이 서비스 JWT를 발급한다")
        void logsInExistingUser() {
            Map<String, Object> claims = Map.of("sub", "apple-user-1", "email", "user@test.com");
            given(appleJwtValidator.validate("identity-token")).willReturn(claims);
            given(appleJwtValidator.getSubject(claims)).willReturn("apple-user-1");
            given(userRepository.findByProviderAndProviderUserId(Provider.APPLE, "apple-user-1"))
                    .willReturn(Optional.of(appUser(10L, "user@test.com", "기존닉네임")));
            stubTokens();

            TokenResponse response = authService.loginWithApple(
                    new AppleLoginReq("identity-token", "auth-code", "무시되는이름"));

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(3600L);
            verify(userService, never()).createUser(any(), any(), any(), any());
            verify(appleAuthService).syncRefreshToken(any(AppUser.class), eq("auth-code"));
        }

        @Test
        @DisplayName("신규 회원이면 Apple sub·email·fullName으로 등록한 뒤 JWT를 발급한다")
        void registersNewUserWithFullNameAndEmail() {
            Map<String, Object> claims = Map.of("sub", "apple-user-1", "email", "user@test.com");
            given(appleJwtValidator.validate("identity-token")).willReturn(claims);
            given(appleJwtValidator.getSubject(claims)).willReturn("apple-user-1");
            given(appleJwtValidator.getEmail(claims)).willReturn("user@test.com");
            given(userRepository.findByProviderAndProviderUserId(Provider.APPLE, "apple-user-1"))
                    .willReturn(Optional.empty());
            given(userService.createUser(Provider.APPLE, "apple-user-1", "user@test.com", "윤시진"))
                    .willReturn(appUser(11L, "user@test.com", "윤시진"));
            stubTokens();

            TokenResponse response = authService.loginWithApple(
                    new AppleLoginReq("identity-token", null, "윤시진"));

            assertThat(response.accessToken()).isEqualTo("access-token");
            verify(userService).createUser(Provider.APPLE, "apple-user-1", "user@test.com", "윤시진");
            verify(userRepository).flush();
            verify(appleAuthService, never()).syncRefreshToken(any(), any());
        }

        @Test
        @DisplayName("첫 로그인이 아니어서 email이 없으면 sub 기반 placeholder email로 등록한다")
        void registersNewUserWithPlaceholderEmailWhenAppleOmitsEmail() {
            Map<String, Object> claims = Map.of("sub", "apple-user-1");
            given(appleJwtValidator.validate("identity-token")).willReturn(claims);
            given(appleJwtValidator.getSubject(claims)).willReturn("apple-user-1");
            given(appleJwtValidator.getEmail(claims)).willReturn(null);
            given(userRepository.findByProviderAndProviderUserId(Provider.APPLE, "apple-user-1"))
                    .willReturn(Optional.empty());
            given(userService.createUser(eq(Provider.APPLE), eq("apple-user-1"),
                    eq("apple-user-1@privaterelay.appleid.com"), eq(null)))
                    .willReturn(appUser(12L, "apple-user-1@privaterelay.appleid.com", "생성된닉네임"));
            stubTokens();

            authService.loginWithApple(new AppleLoginReq("identity-token", null, null));

            verify(userService).createUser(Provider.APPLE, "apple-user-1",
                    "apple-user-1@privaterelay.appleid.com", null);
        }

        @Test
        @DisplayName("authorizationCode 교환이 실패해도 로그인은 성공한다")
        void loginSucceedsWhenTokenExchangeFails() {
            Map<String, Object> claims = Map.of("sub", "apple-user-1", "email", "user@test.com");
            given(appleJwtValidator.validate("identity-token")).willReturn(claims);
            given(appleJwtValidator.getSubject(claims)).willReturn("apple-user-1");
            AppUser user = appUser(10L, "user@test.com", "기존닉네임");
            given(userRepository.findByProviderAndProviderUserId(Provider.APPLE, "apple-user-1"))
                    .willReturn(Optional.of(user));
            willThrow(new BusinessException(ErrorCode.AUTH_APPLE_TOKEN_EXCHANGE_FAILED))
                    .given(appleAuthService).syncRefreshToken(user, "auth-code");
            stubTokens();

            TokenResponse response = authService.loginWithApple(
                    new AppleLoginReq("identity-token", "auth-code", null));

            assertThat(response.accessToken()).isEqualTo("access-token");
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("Apple 회원이면 revoke 후 로컬 계정을 삭제한다")
        void revokesAppleThenDeletesAccount() {
            AppUser user = appUser(10L, "user@test.com", "시진");
            given(userRepository.findById(10L)).willReturn(Optional.of(user));

            WithdrawResponse response = authService.withdraw(user);

            assertThat(response.withdrawn()).isTrue();
            verify(appleAuthService).revoke(user);
            verify(userWithdrawalService).deleteAccount(user);
        }

        @Test
        @DisplayName("Apple revoke가 실패하면 로컬 계정을 삭제하지 않는다")
        void doesNotDeleteWhenAppleRevokeFails() {
            AppUser user = appUser(10L, "user@test.com", "시진");
            given(userRepository.findById(10L)).willReturn(Optional.of(user));
            willThrow(new BusinessException(ErrorCode.AUTH_APPLE_REVOKE_FAILED))
                    .given(appleAuthService).revoke(user);

            assertThatThrownBy(() -> authService.withdraw(user))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_APPLE_REVOKE_FAILED));

            verify(userWithdrawalService, never()).deleteAccount(any());
        }

        @Test
        @DisplayName("Apple이 아닌 회원은 revoke 없이 로컬 계정만 삭제한다")
        void deletesNonAppleUserWithoutRevoke() {
            AppUser googleUser = AppUser.builder()
                    .provider(Provider.GOOGLE)
                    .providerUserId("google-uid")
                    .email("g@test.com")
                    .nickname("구글유저")
                    .build();
            ReflectionTestUtils.setField(googleUser, "id", 20L);
            given(userRepository.findById(20L)).willReturn(Optional.of(googleUser));

            authService.withdraw(googleUser);

            verify(appleAuthService, never()).revoke(any());
            verify(userWithdrawalService).deleteAccount(googleUser);
        }
    }

    private void stubTokens() {
        given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("refresh-token");
        given(jwtTokenProvider.getAccessTokenValiditySeconds()).willReturn(3600L);
    }

    private AppUser appUser(Long id, String email, String nickname) {
        AppUser user = AppUser.builder()
                .provider(Provider.APPLE)
                .providerUserId("apple-user-1")
                .email(email)
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
