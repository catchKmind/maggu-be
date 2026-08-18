package com.maggu.maggu.community.entity;

import com.maggu.maggu.global.entity.BaseEntity;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.user.entity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    private static final int MAX_REPORT_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 탈퇴 시 SET NULL → 댓글은 존속, "탈퇴한 회원"으로 렌더
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    // null이면 최상위 댓글. 대댓글의 대댓글은 서비스 계층에서 금지(1단계 제한)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Column(nullable = false, length = 120)
    private String content;

    // 원자적 UPDATE로만 증감할 것. 엔티티 세터로 갱신 금지 — 동시 공감 시 값이 유실됨
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "report_count", nullable = false)
    private int reportCount;

    @Column(nullable = false)
    private boolean deleted;

    @Builder
    public Comment(Post post, AppUser user, Comment parentComment, String content) {
        this.post = post;
        this.user = user;
        this.parentComment = parentComment;
        this.content = content;
        this.likeCount = 0;
        this.reportCount = 0;
        this.deleted = false;
    }

    public boolean isReply() {
        return this.parentComment != null;
    }

    public boolean shouldAutoHide() {
        return this.reportCount >= MAX_REPORT_COUNT;
    }

    public void markDeleted() {
        this.deleted = true;
    }
}