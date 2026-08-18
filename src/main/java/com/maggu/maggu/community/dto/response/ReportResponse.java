package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportResponse {
    private Long postId;
    private Long commentId;
    private boolean reported;
    private boolean autoHidden;
}