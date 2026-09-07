package com.maggu.maggu.auth.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "apple")
public record AppleProperties(
        String bundleId,
        String teamId,
        String keyId,
        String keyPath
) {
}
