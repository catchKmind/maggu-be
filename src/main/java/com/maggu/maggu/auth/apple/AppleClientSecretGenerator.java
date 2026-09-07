package com.maggu.maggu.auth.apple;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class AppleClientSecretGenerator {

    static final String APPLE_AUD = "https://appleid.apple.com";
    static final Duration CLIENT_SECRET_TTL = Duration.ofDays(30);

    private final AppleProperties appleProperties;
    private final ResourceLoader resourceLoader;
    private volatile ECPrivateKey privateKey;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public AppleClientSecretGenerator(AppleProperties appleProperties, ResourceLoader resourceLoader) {
        this.appleProperties = appleProperties;
        this.resourceLoader = resourceLoader;
    }

    public String create() {
        try {
            Instant now = Instant.now();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.ES256)
                            .keyID(appleProperties.keyId())
                            .build(),
                    new JWTClaimsSet.Builder()
                            .issuer(appleProperties.teamId())
                            .issueTime(Date.from(now))
                            .expirationTime(Date.from(now.plus(CLIENT_SECRET_TTL)))
                            .audience(APPLE_AUD)
                            .subject(appleProperties.bundleId())
                            .build()
            );
            jwt.sign(new ECDSASigner(loadPrivateKey()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Apple client secret 서명에 실패했습니다.");
        }
    }

    private ECPrivateKey loadPrivateKey() {
        ECPrivateKey cached = privateKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (privateKey == null) {
                privateKey = parsePrivateKey(resolveKeyResource());
            }
            return privateKey;
        }
    }

    private Resource resolveKeyResource() {
        if (!StringUtils.hasText(appleProperties.keyPath())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "apple.key-path가 설정되지 않았습니다.");
        }
        Resource resource = resourceLoader.getResource(appleProperties.keyPath());
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Apple .p8 키 파일을 찾을 수 없습니다.");
        }
        return resource;
    }

    static ECPrivateKey parsePrivateKey(Resource resource) {
        try (PEMParser pemParser = new PEMParser(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            Object parsed = pemParser.readObject();
            if (parsed == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Apple .p8 키가 비어 있습니다.");
            }
            PrivateKeyInfo keyInfo = toPrivateKeyInfo(parsed);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);
            return (ECPrivateKey) converter.getPrivateKey(keyInfo);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Apple .p8 키 파일을 읽지 못했습니다.");
        }
    }

    private static PrivateKeyInfo toPrivateKeyInfo(Object parsed) {
        if (parsed instanceof PrivateKeyInfo privateKeyInfo) {
            return privateKeyInfo;
        }
        if (parsed instanceof PEMKeyPair pemKeyPair) {
            return pemKeyPair.getPrivateKeyInfo();
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Apple .p8 키 형식을 해석하지 못했습니다.");
    }
}
