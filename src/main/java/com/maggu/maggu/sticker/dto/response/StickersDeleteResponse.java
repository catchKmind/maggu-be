package com.maggu.maggu.sticker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record StickersDeleteResponse(
        @Schema(description = "삭제 요청한 스티커 ID 목록 (존재하지 않거나 이미 삭제된 ID도 포함)",
                example = "[1, 2, 3]")
        List<Long> stickerIds
) {

    public static StickersDeleteResponse from(List<Long> stickerIds) {
        return StickersDeleteResponse.builder()
                .stickerIds(stickerIds)
                .build();
    }
}
