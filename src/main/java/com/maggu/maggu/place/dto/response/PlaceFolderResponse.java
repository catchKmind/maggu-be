package com.maggu.maggu.place.dto.response;

import com.maggu.maggu.place.entity.PlaceFolder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PlaceFolderResponse(
        @Schema(description = "장소 폴더 ID")
        Long placeFolderId,

        @Schema(description = "폴더명")
        String name,

        @Schema(description = "아이콘")
        String icon,

        @Schema(description = "기본 폴더 여부")
        boolean isDefault
) {
    public static PlaceFolderResponse from(PlaceFolder placeFolder) {
        return PlaceFolderResponse.builder()
                .placeFolderId(placeFolder.getId())
                .name(placeFolder.getName())
                .icon(placeFolder.getIcon())
                .isDefault(placeFolder.isDefault())
                .build();
    }
}
