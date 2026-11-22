package com.maggu.maggu.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StickerReactionRequest {

    @NotNull
    @Schema(description = "스티커 ID", example = "1")
    private Long stickerId;
}