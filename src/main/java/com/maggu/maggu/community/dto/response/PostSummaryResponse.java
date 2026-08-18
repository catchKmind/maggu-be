package com.maggu.maggu.community.dto.response;

import com.maggu.maggu.community.entity.PostCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class PostSummaryResponse {
    private Long postId;
    private String slug;
    private String writerNickname;
    private String content;
    private List<String> imageUrls;
    private String placeName;
    private PostCategory category;

    private long reactionCount;
    private boolean reactedByMe;

    private int scrapCount;
    private long commentCount;
    private boolean scrappedByMe;
    private Instant createdAt;
}