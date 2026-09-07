package com.maggu.maggu.auth.apple;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppleClientSecretGeneratorTest {

    private static final String KEY_PATH = "classpath:AppleSignInsKey.p8";

    @Mock
    private ResourceLoader resourceLoader;

    @Test
    @DisplayName("PEMParser로 .p8을 읽어 ES256 client_secret JWT를 생성하고 유효기간은 30일이다")
    void createsEs256ClientSecretWithThirtyDayTtl() throws Exception {
        KeyPair keyPair = generateEcKeyPair();
        String pem = toPkcs8Pem(keyPair);
        given(resourceLoader.getResource(KEY_PATH))
                .willReturn(new ByteArrayResource(pem.getBytes(StandardCharsets.UTF_8)));

        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(
                new AppleProperties("com.maggu.app", "TEAMID1234", "KEYID12345", KEY_PATH),
                resourceLoader
        );

        String clientSecret = generator.create();
        SignedJWT jwt = SignedJWT.parse(clientSecret);
        Date issuedAt = jwt.getJWTClaimsSet().getIssueTime();
        Date expiresAt = jwt.getJWTClaimsSet().getExpirationTime();

        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.ES256);
        assertThat(jwt.getHeader().getKeyID()).isEqualTo("KEYID12345");
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("TEAMID1234");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("com.maggu.app");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly(AppleClientSecretGenerator.APPLE_AUD);
        assertThat(Duration.between(issuedAt.toInstant(), expiresAt.toInstant()))
                .isEqualTo(AppleClientSecretGenerator.CLIENT_SECRET_TTL);
        assertThat(AppleClientSecretGenerator.CLIENT_SECRET_TTL).isLessThanOrEqualTo(Duration.ofDays(30));
    }

    private static KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static String toPkcs8Pem(KeyPair keyPair) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(keyPair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n";
    }
}
