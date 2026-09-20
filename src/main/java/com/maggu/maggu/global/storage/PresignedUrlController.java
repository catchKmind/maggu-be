package com.maggu.maggu.global.storage;

import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Uploads", description = "이미지 업로드 관련 API")
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class PresignedUrlController {

    private final PresignedUrlService presignedUrlService;

    @Operation(summary = "이미지 업로드용 presigned URL 발급",
            description = "이미지를 S3에 업로드할 수 있는 임시 PUT URL을 발급한다." +
                    " FE는 응답의 presignedUrl로 파일을 PUT한 뒤, objectKey를 스티커/게시글 생성 API의 imageUrl로 전달하면 된다." +
                    " URL은 발급 후 5분간만 유효하다.")
    @PostMapping("/presigned-url")
    public PresignedUrlResponse generatePresignedUrl(
            @CurrentUser AppUser user,
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        return presignedUrlService.generatePresignedUrl(user, request);
    }
}
