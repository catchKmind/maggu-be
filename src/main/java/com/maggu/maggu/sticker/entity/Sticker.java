package com.maggu.maggu.sticker.entity;

import com.maggu.maggu.global.entity.BaseEntity;
import com.maggu.maggu.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "sticker",
        uniqueConstraints = @UniqueConstraint(name = "uq_sticker_giphy_id", columnNames = "giphy_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sticker extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    // CUSTOM/GIPHY/MASTER 구분
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StickerType type;

    // FK 아님
    // GIPHY 타입일 때만 값이 있고 find-or-create로 다뤄야 함
    // 같은 GIF를 고른 유저들이 같은 row를 공유해야 post_sticker_reaction 집계(GROUP BY sticker_id)가 유저에 걸쳐 합산됨
    @Column(name = "giphy_id", length = 50)
    private String giphyId;

    @Column(nullable = false)
    private boolean deleted = false;

    public void markDeleted() {
        this.deleted = true;
    }

    public boolean isOwnedBy(AppUser candidate) {
        if (this.user == null || candidate == null) {
            return false;
        }
        return this.user.getId().equals(candidate.getId());
    }

    public boolean isCustom() {
        return this.type == StickerType.CUSTOM;
    }

    @Builder
    public Sticker(String name, String imageUrl, AppUser user, StickerType type, String giphyId) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.user = user;
        this.type = type;
        this.giphyId = giphyId;
    }
}
