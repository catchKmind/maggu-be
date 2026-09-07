package com.maggu.maggu.auth.apple;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AppleAuthServiceTest {

    @Mock
    private AppleAuthClient appleAuthClient;

    @InjectMocks
    private AppleAuthService appleAuthService;

    @Nested
    @DisplayName("syncRefreshToken")
    class SyncRefreshToken {

        @Test
        @DisplayName("authorizationCode가 있으면 refresh token으로 교환해 저장한다")
        void exchangesAuthorizationCodeAndStoresRefreshToken() {
            AppUser user = appleUser();
            given(appleAuthClient.exchangeAuthorizationCode("auth-code")).willReturn("refresh-token");

            appleAuthService.syncRefreshToken(user, "auth-code");

            assertThat(user.getAppleRefreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("authorizationCode가 없으면 Apple 서버를 호출하지 않는다")
        void skipsWhenAuthorizationCodeMissing() {
            AppUser user = appleUser();

            appleAuthService.syncRefreshToken(user, " ");

            verifyNoInteractions(appleAuthClient);
            assertThat(user.getAppleRefreshToken()).isNull();
        }
    }

    @Nested
    @DisplayName("revoke")
    class Revoke {

        @Test
        @DisplayName("저장된 refresh token으로 Apple 연동을 해제한다")
        void revokesStoredRefreshToken() {
            AppUser user = appleUser();
            user.updateAppleRefreshToken("refresh-token");

            appleAuthService.revoke(user);

            verify(appleAuthClient).revokeRefreshToken("refresh-token");
        }

        @Test
        @DisplayName("refresh token이 없으면 revoke를 건너뛴다")
        void skipsWhenRefreshTokenMissing() {
            appleAuthService.revoke(appleUser());

            verify(appleAuthClient, never()).revokeRefreshToken(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("Apple 회원이 아니면 revoke를 건너뛴다")
        void skipsNonAppleUsers() {
            AppUser googleUser = AppUser.builder()
                    .provider(Provider.GOOGLE)
                    .providerUserId("google-uid")
                    .email("g@test.com")
                    .nickname("구글유저")
                    .build();
            ReflectionTestUtils.setField(googleUser, "id", 2L);

            appleAuthService.revoke(googleUser);

            verifyNoInteractions(appleAuthClient);
        }
    }

    private AppUser appleUser() {
        AppUser user = AppUser.builder()
                .provider(Provider.APPLE)
                .providerUserId("apple-user-1")
                .email("user@test.com")
                .nickname("시진")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
