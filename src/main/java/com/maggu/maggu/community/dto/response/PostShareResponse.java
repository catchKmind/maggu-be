package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostShareResponse {
    private Long postId;
    private String url;
}