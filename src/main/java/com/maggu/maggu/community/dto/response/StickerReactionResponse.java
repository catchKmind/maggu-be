package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StickerReactionResponse {
    private Long postId;
    private String myReactionSticker; // 취소된 경우 null
    private Long myReactionStickerId;
    private String myReactionStickerImageUrl;
    private List<StickerReactionItem> stickerReactions;
}