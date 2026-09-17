package com.maggu.maggu.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud.aws.cloudfront")
public record CloudFrontProperties(
        String domain
) {
}
