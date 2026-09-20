package com.maggu.maggu.global.storage;

import lombok.Builder;

@Builder
public record PresignedUrlResponse(
        String presignedUrl,

        String objectKey
) {
}
