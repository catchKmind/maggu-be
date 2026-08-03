package com.maggu.maggu.community.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StickerReactionRequest {

    @NotNull
    private Long stickerId;
}