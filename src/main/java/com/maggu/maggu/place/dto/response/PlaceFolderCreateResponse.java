package com.maggu.maggu.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PlaceFolderCreateResponse(
        @Schema(description = "id")
        Long placeFolderId,

        @Schema(description = "폴더명")
        String name,

        @Schema(description = "아이콘")
        String icon
) {
}
