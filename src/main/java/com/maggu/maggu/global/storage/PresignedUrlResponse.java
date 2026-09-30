package com.maggu.maggu.global.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PresignedUrlResponse(
        @Schema(description = "S3 업로드(PUT)용 임시 URL. 저장하지 말 것")
        String presignedUrl,

        @Schema(description = "스티커/게시글 생성 API의 imageUrl로 전달할 값")
        String objectKey
) {
}
