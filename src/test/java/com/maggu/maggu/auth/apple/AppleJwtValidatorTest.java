package com.maggu.maggu.auth.apple;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppleJwtValidatorTest {

    private static final String BUNDLE_ID = "com.maggu.app";
    private static final String KID = "kid-1";

    @Mock
    private AppleJwkSetClient appleJwkSetClient;

    private AppleJwtValidator validator;
    private RSAKey rsaKey;

    @BeforeEach
    void setUp() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID(KID).generate();
        validator = new AppleJwtValidator(new AppleProperties(BUNDLE_ID, "TEAMID1234", "KEYID12345", "classpath:AppleSignInsKey.p8"), appleJwkSetClient);
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("서명·iss·aud(문자열)·exp가 유효하면 claims를 반환하고 sub/email을 추출한다")
        void returnsClaimsWhenTokenIsValid() throws Exception {
            given(appleJwkSetClient.fetch()).willReturn(new JWKSet(rsaKey));
            String token = signedToken(rsaKey, KID, AppleJwtValidator.APPLE_ISS, BUNDLE_ID,
                    future(3600), "apple-user-1", "user@privaterelay.appleid.com");

            Map<String, Object> claims = validator.validate(token);

            assertThat(validator.getSubject(claims)).isEqualTo("apple-user-1");
            assertThat(validator.getEmail(claims)).isEqualTo("user@privaterelay.appleid.com");
        }

        @Test
        @DisplayName("aud가 배열이어도 Bundle ID가 포함되면 통과한다")
        void acceptsAudienceListContainingBundleId() throws Exception {
            given(appleJwkSetClient.fetch()).willReturn(new JWKSet(rsaKey));
            String token = signedToken(rsaKey, KID, AppleJwtValidator.APPLE_ISS,
                    List.of("com.other.app", BUNDLE_ID),
                    future(3600), "apple-user-1", "user@test.com");

            Map<String, Object> claims = validator.validate(token);

            assertThat(validator.getSubject(claims)).isEqualTo("apple-user-1");
        }

        @Test
        @DisplayName("같은 kid는 공개키를 재조회하지 않는다")
        void cachesPublicKeyByKid() throws Exception {
            given(appleJwkSetClient.fetch()).willReturn(new JWKSet(rsaKey));
            String token = signedToken(rsaKey, KID, AppleJwtValidator.APPLE_ISS, BUNDLE_ID,
                    future(3600), "apple-user-1", "user@test.com");

            validator.validate(token);
            validator.validate(token);

            verify(appleJwkSetClient, times(1)).fetch();
        }

        @Test
        @DisplayName("캐시에 없는 kid가 들어오면 공개키를 다시 조회한다")
        void refreshesKeysWhenKidIsMissing() throws Exception {
            RSAKey secondKey = new RSAKeyGenerator(2048).keyID("kid-2").generate();
            given(appleJwkSetClient.fetch())
                    .willReturn(new JWKSet(rsaKey), new JWKSet(secondKey));

            String firstToken = signedToken(rsaKey, KID, AppleJwtValidator.APPLE_ISS, BUNDLE_ID,
                    future(3600), "apple-user-1", "user@test.com");
            String secondToken = signedToken(secondKey, "kid-2", AppleJwtValidator.APPLE_ISS, BUNDLE_ID,
                    future(3600), "apple-user-2", "user2@test.com");

            validator.validate(firstToken);
            Map<String, Object> claims = validator.validate(secondToken);

            assertThat(validator.getSubject(claims)).isEqualTo("apple-user-2");
            verify(appleJwkSetClient, times(2)).fetch();
        }

        @Test
        @DisplayName("iss가 Apple이 아니면 예외를 던진다")
        void rejectsWrongIssuer() throws Exception {
            given(appleJwkSetClient.fetch()).willReturn(new JWKSet(rsaKey));
            String token = signedToken(rsaKey, KID, "https://evil.example.com", BUNDLE_ID,
                    future(3600), "apple-user-1", "user@test.com");

            assertThatThrownBy(() -> validator.validate(token))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_TOKEN));
        }

        @Test
        @DisplayName("aud가 Bundle ID와 다르면 예외를 던진다")
        void rejectsWrongAudience() throws Exception {
            given(appleJwkSetClient.fetch()).willReturn(new JWKSet(rsaKey));
            String token = signedToken(rsaKey, KID, AppleJwtValidator.APPLE_ISS, "com.other.app",
                    future(3600), "apple-user-1", "user@test.com");

            assertThatThrownBy(() -> validator.validate(token))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_TOKEN));
        }

        @Test
        @DisplayName("만료된 토큰이면 AUTH_EXPIRED_TOKEN 예외를 던진다")
        void rejectsExpiredToken() throws Exception {
            given(appleJwkSetClient.fetch()).willReturn(new JWKSet(rsaKey));
            String token = signedToken(rsaKey, KID, AppleJwtValidator.APPLE_ISS, BUNDLE_ID,
                    new Date(System.currentTimeMillis() - 5_000), "apple-user-1", "user@test.com");

            assertThatThrownBy(() -> validator.validate(token))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_EXPIRED_TOKEN));
        }

        @Test
        @DisplayName("서명이 유효하지 않으면 예외를 던진다")
        void rejectsInvalidSignature() throws Exception {
            RSAKey otherKey = new RSAKeyGenerator(2048).keyID(KID).generate();
            given(appleJwkSetClient.fetch()).willReturn(new JWKSet(rsaKey));
            String token = signedToken(otherKey, KID, AppleJwtValidator.APPLE_ISS, BUNDLE_ID,
                    future(3600), "apple-user-1", "user@test.com");

            assertThatThrownBy(() -> validator.validate(token))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_TOKEN));
        }
    }

    private static Date future(int seconds) {
        return new Date(System.currentTimeMillis() + seconds * 1000L);
    }

    private static String signedToken(
            RSAKey signingKey,
            String kid,
            String issuer,
            Object audience,
            Date expiration,
            String subject,
            String email
    ) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .expirationTime(expiration)
                .subject(subject)
                .claim("email", email);

        if (audience instanceof String aud) {
            claims.audience(aud);
        } else if (audience instanceof List<?> audList) {
            @SuppressWarnings("unchecked")
            List<String> audiences = (List<String>) audList;
            claims.audience(audiences);
        }

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(),
                claims.build()
        );
        signedJwt.sign(new RSASSASigner(signingKey));
        return signedJwt.serialize();
    }
}
