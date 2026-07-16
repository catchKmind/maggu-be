package com.maggu.maggu.global.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "post_sticker_reaction",
        uniqueConstraints = @UniqueConstraint(name = "uq_reaction_post_user", columnNames = {"post_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostStickerReaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // 반응이 달린 스티커는 마스터에서 삭제 불가(RESTRICT).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sticker_id", nullable = false)
    private Sticker sticker;

    @Builder
    public PostStickerReaction(Post post, AppUser user, Sticker sticker) {
        this.post = post;
        this.user = user;
        this.sticker = sticker;
    }

    // 유저당 게시물당 스티커 1개 — 변경은 DELETE+INSERT가 아니라 이 필드 UPDATE로 처리.
    public void changeSticker(Sticker sticker) {
        this.sticker = sticker;
    }
}
