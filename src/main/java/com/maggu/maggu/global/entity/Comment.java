package com.maggu.maggu.global.entity;

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

    @Builder
    public Comment(Post post, AppUser user, Comment parentComment, String content) {
        this.post = post;
        this.user = user;
        this.parentComment = parentComment;
        this.content = content;
        this.likeCount = 0;
    }
}
