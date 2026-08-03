package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScrapResponse {
    private Long postId;
    private Long folderId;
    private boolean scrapped;
}