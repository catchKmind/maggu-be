package com.maggu.maggu.auth.apple;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;

@Component
public class RemoteAppleJwkSetClient implements AppleJwkSetClient {

    static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";

    @Override
    public JWKSet fetch() {
        try {
            return JWKSet.load(URI.create(APPLE_KEYS_URL).toURL());
        } catch (IOException | ParseException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Apple 공개키를 조회하지 못했습니다.");
        }
    }
}
