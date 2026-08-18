package com.maggu.maggu.community.dto.response;

import com.maggu.maggu.community.entity.PostCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class PostDetailResponse {
    private Long postId;
    private String slug;
    private String writerNickname;
    private String content;
    private List<String> imageUrls;
    private String placeName;
    private Double latitude;
    private Double longitude;
    private String tourismContentId;
    private PostCategory category;
    private int scrapCount;
    private boolean scrappedByMe;
    private Map<String, Long> stickerReactionCounts;
    private String myReactionSticker;
    private Instant createdAt;
    private Instant updatedAt;
}