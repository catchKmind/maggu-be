package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostDeleteResponse {
    private Long postId;
    private boolean deleted;
}