package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentCreateResponse {
    private Long commentId;
    private Long parentCommentId;
}