package com.maggu.maggu.community.dto.request;

import com.maggu.maggu.community.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "본문", example = "해운대 다녀왔어요")
        String content,

        @NotNull
        @Schema(description = "카테고리", example = "RECOMMEND")
        PostCategory category,

        @Size(max = 4, message = "사진은 최대 4장까지 첨부할 수 있습니다.")
        @Schema(description = "사진 URL 0~4장. 없으면 생략", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<String> imageUrls,

        @Schema(description = "장소명", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String placeName,

        @Schema(description = "관광공사 콘텐츠 ID", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String tourismContentId,

        @Schema(description = "위치 출처 AUTO(EXIF) / MANUAL(지도 검색)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocationSource locationSource,

        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        @Schema(description = "위도. 사진이 있으면 필수", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Double latitude,

        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        @Schema(description = "경도. 사진이 있으면 필수", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Double longitude
) {
    public enum LocationSource {
        AUTO, MANUAL
    }
}
