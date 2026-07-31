package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StickerReactionResponse {
    private Long postId;
    private String myReactionSticker; // 취소된 경우 null
}