package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentLikeResponse {
    private Long commentId;
    private boolean liked;
}