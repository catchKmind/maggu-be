package com.maggu.maggu.auth.apple;

import com.nimbusds.jose.jwk.JWKSet;

public interface AppleJwkSetClient {

    JWKSet fetch();
}
