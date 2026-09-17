package com.maggu.maggu.global.storage;

import com.maggu.maggu.global.config.CloudFrontProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

@Component
public class CloudFrontUrlResolver {

    private final String cdnBaseUrl;
    private final String bucket;

    public CloudFrontUrlResolver(
            CloudFrontProperties cloudFrontProperties,
            @Value("${cloud.aws.s3.bucket}") String bucket
    ) {
        this.cdnBaseUrl = normalizeBaseUrl(cloudFrontProperties == null ? null : cloudFrontProperties.domain());
        this.bucket = bucket == null ? "" : bucket;
    }

    public String toPublicUrl(String stored) {
        if (!StringUtils.hasText(stored) || !StringUtils.hasText(cdnBaseUrl)) {
            return stored;
        }
        if (stored.startsWith(cdnBaseUrl)) {
            return stored;
        }
        String objectKey = extractObjectKey(stored);
        if (!StringUtils.hasText(objectKey)) {
            return stored;
        }
        return cdnBaseUrl + "/" + objectKey;
    }

    private String extractObjectKey(String stored) {
        if (!stored.contains("://")) {
            return trimLeadingSlash(stored);
        }

        String s3SchemePrefix = "s3://" + bucket + "/";
        if (StringUtils.hasText(bucket) && stored.startsWith(s3SchemePrefix)) {
            return stored.substring(s3SchemePrefix.length());
        }

        URI uri;
        try {
            uri = URI.create(stored);
        } catch (IllegalArgumentException e) {
            return null;
        }

        String host = uri.getHost();
        String path = uri.getPath();
        if (!StringUtils.hasText(host) || !isOurS3Host(host)) {
            return null;
        }

        String objectPath = path == null ? "" : path;
        String pathStylePrefix = "/" + bucket + "/";
        if (isPathStyleS3Host(host) && objectPath.startsWith(pathStylePrefix)) {
            return objectPath.substring(pathStylePrefix.length());
        }
        return trimLeadingSlash(objectPath);
    }

    private boolean isOurS3Host(String host) {
        if (!StringUtils.hasText(bucket)) {
            return false;
        }
        if (host.equals(bucket + ".s3.amazonaws.com")
                || (host.startsWith(bucket + ".s3.") && host.endsWith(".amazonaws.com"))) {
            return true;
        }
        return isPathStyleS3Host(host);
    }

    private boolean isPathStyleS3Host(String host) {
        return "s3.amazonaws.com".equals(host) || (host.startsWith("s3.") && host.endsWith(".amazonaws.com"));
    }

    private static String trimLeadingSlash(String value) {
        int index = 0;
        while (index < value.length() && value.charAt(index) == '/') {
            index++;
        }
        return value.substring(index);
    }

    private static String normalizeBaseUrl(String domain) {
        if (!StringUtils.hasText(domain)) {
            return "";
        }
        String normalized = domain.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        return normalized;
    }
}
