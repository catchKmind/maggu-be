package com.maggu.maggu.auth.apple;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class AppleJwtValidator {

    static final String APPLE_ISS = "https://appleid.apple.com";

    private static final String CLAIM_SUB = "sub";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_AUD = "aud";

    private final String bundleId;
    private final AppleJwkSetClient appleJwkSetClient;
    private final Cache<String, RSAKey> publicKeyCache;
    private final Object refreshLock = new Object();

    public AppleJwtValidator(AppleProperties appleProperties, AppleJwkSetClient appleJwkSetClient) {
        this.bundleId = appleProperties.bundleId();
        this.appleJwkSetClient = appleJwkSetClient;
        this.publicKeyCache = Caffeine.newBuilder()
                .maximumSize(20)
                .build();
    }

    /**
     * Apple identity token의 서명·iss·aud·exp를 검증하고 Claims 맵을 반환한다.
     */
    public Map<String, Object> validate(String identityToken) {
        if (!StringUtils.hasText(identityToken)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        SignedJWT signedJwt = parseSignedJwt(identityToken);
        verifySignature(signedJwt);

        JWTClaimsSet claimsSet = parseClaimsSet(signedJwt);
        verifyIssuer(claimsSet);
        verifyAudience(claimsSet);
        verifyExpiration(claimsSet);

        return Collections.unmodifiableMap(new LinkedHashMap<>(claimsSet.getClaims()));
    }

    public String getSubject(Map<String, Object> claims) {
        return stringClaim(claims, CLAIM_SUB);
    }

    public String getEmail(Map<String, Object> claims) {
        return stringClaim(claims, CLAIM_EMAIL);
    }

    private SignedJWT parseSignedJwt(String identityToken) {
        try {
            return SignedJWT.parse(identityToken);
        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private void verifySignature(SignedJWT signedJwt) {
        if (!JWSAlgorithm.RS256.equals(signedJwt.getHeader().getAlgorithm())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        String kid = signedJwt.getHeader().getKeyID();
        if (!StringUtils.hasText(kid)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        RSAKey rsaKey = resolvePublicKey(kid);
        try {
            if (!signedJwt.verify(new RSASSAVerifier(rsaKey.toRSAPublicKey()))) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
            }
        } catch (JOSEException e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private RSAKey resolvePublicKey(String kid) {
        RSAKey cached = publicKeyCache.getIfPresent(kid);
        if (cached != null) {
            return cached;
        }

        synchronized (refreshLock) {
            RSAKey existing = publicKeyCache.getIfPresent(kid);
            if (existing != null) {
                return existing;
            }
            refreshPublicKeys();
            RSAKey refreshed = publicKeyCache.getIfPresent(kid);
            if (refreshed == null) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN, "Apple 공개키를 찾을 수 없습니다.");
            }
            return refreshed;
        }
    }

    private void refreshPublicKeys() {
        log.info("Apple JWKSet을 갱신합니다.");
        JWKSet jwkSet = appleJwkSetClient.fetch();
        for (JWK jwk : jwkSet.getKeys()) {
            if (jwk instanceof RSAKey rsaKey && StringUtils.hasText(rsaKey.getKeyID())) {
                publicKeyCache.put(rsaKey.getKeyID(), rsaKey);
            }
        }
    }

    private JWTClaimsSet parseClaimsSet(SignedJWT signedJwt) {
        try {
            return signedJwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private void verifyIssuer(JWTClaimsSet claimsSet) {
        if (!APPLE_ISS.equals(claimsSet.getIssuer())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    /**
     * Apple identity token의 aud는 단일 문자열(Bundle ID) 또는 배열로 올 수 있다.
     */
    private void verifyAudience(JWTClaimsSet claimsSet) {
        if (containsAudience(claimsSet.getAudience()) || containsAudience(claimsSet.getClaim(CLAIM_AUD))) {
            return;
        }
        throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }

    private boolean containsAudience(Object aud) {
        if (aud instanceof String audString) {
            return bundleId.equals(audString);
        }
        if (aud instanceof Collection<?> audList) {
            return audList.stream().anyMatch(value -> bundleId.equals(String.valueOf(value)));
        }
        return false;
    }

    private void verifyExpiration(JWTClaimsSet claimsSet) {
        Date expirationTime = claimsSet.getExpirationTime();
        if (expirationTime == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        if (!expirationTime.after(new Date())) {
            throw new BusinessException(ErrorCode.AUTH_EXPIRED_TOKEN);
        }
    }

    private String stringClaim(Map<String, Object> claims, String claimName) {
        if (claims == null) {
            return null;
        }
        Object value = claims.get(claimName);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : null;
    }
}
