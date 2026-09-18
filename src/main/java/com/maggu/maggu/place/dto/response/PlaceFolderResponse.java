package com.maggu.maggu.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PlaceFolderResponse(
        @Schema(description = "id")
        Long placeFolderId,

        @Schema(description = "폴더명")
        String name,

        @Schema(description = "아이콘")
        String icon,

        @Schema(description = "기본 폴더 여부")
        boolean isDefault
) {
}
