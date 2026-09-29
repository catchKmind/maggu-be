package com.maggu.maggu.global.storage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    @Value("${upload.presigned-url-expiration-seconds}")
    private long expirationSeconds;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/heic", "image/webp");
    private static final String FILE_NAME_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\."
            + ALLOWED_CONTENT_TYPES.stream()
            .map(type -> type.split("/")[1])
            .collect(Collectors.joining("|", "(", ")"));

    public PresignedUrlResponse generatePresignedUrl(AppUser user, PresignedUrlRequest request) {
        if (!isAllowedContentType(request.contentType())) {
            throw new BusinessException(ErrorCode.UPLOAD_UNSUPPORTED_CONTENT_TYPE);
        }
        UploadDomain domain = UploadDomain.from(request.domain());

        // object key 생성
        String objectKey = buildObjectKey(user, request, domain);

        // Presigned URL 요청 생성
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectKey)
                .withMethod(HttpMethod.PUT)
                .withExpiration(Date.from(Instant.now().plusSeconds(expirationSeconds)))
                .withContentType(request.contentType());


        return PresignedUrlResponse.builder()
                .presignedUrl(amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString())
                .objectKey(objectKey)
                .build();
    }

    // buildObjectKey()가 만든 형식({DOMAIN}/{userId}/{uuid}.{ext})이 아니면 거부
    public void validateObjectKey(AppUser user, UploadDomain domain, String objectKey) {
        String regex = Pattern.quote(domain.name()) + "/" + user.getId() + "/" + FILE_NAME_REGEX;
        if (objectKey == null || !objectKey.matches(regex)) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_OBJECT_KEY);
        }
    }

    private String buildObjectKey(AppUser user, PresignedUrlRequest request, UploadDomain domain) {
        String ext = request.contentType().split("/")[1];
        return domain + "/" + user.getId() + "/" + UUID.randomUUID() + "." + ext;
    }

    private boolean isAllowedContentType(String value) {
        return ALLOWED_CONTENT_TYPES.contains(value);
    }
}
