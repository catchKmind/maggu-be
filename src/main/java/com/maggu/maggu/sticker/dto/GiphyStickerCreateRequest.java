package com.maggu.maggu.sticker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GiphyStickerCreateRequest(
        @NotBlank
        @Size(max = 50)
        String giphyId,

        @NotBlank
        @Size(max = 500)
        String imageUrl
) {
}
