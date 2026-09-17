package com.maggu.maggu.map.dto;

import com.maggu.maggu.map.client.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AutocompleteCandidateResponse(
        @Schema(description = "자동완성 후보의 contentId")
        String contentId,

        @Schema(description = "자동완성 후보의 관광 타입")
        ContentType contentType,

        @Schema(description = "자동완성 후보의 장소명")
        String title
) {
}
