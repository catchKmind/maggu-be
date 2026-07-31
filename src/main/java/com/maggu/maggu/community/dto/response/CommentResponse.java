package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class CommentResponse {
    private Long commentId;
    private String writerNickname; // 탈퇴 회원이면 "탈퇴한 회원"
    private String content;
    private int likeCount;
    private boolean likedByMe;
    private Instant createdAt;
    private List<CommentResponse> replies;
}