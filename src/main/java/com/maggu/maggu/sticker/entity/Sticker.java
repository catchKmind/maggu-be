package com.maggu.maggu.sticker.entity;

import com.maggu.maggu.global.entity.BaseEntity;
import com.maggu.maggu.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sticker")
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

    @Column(nullable = false)
    private boolean deleted = false;

    public void markDeleted() {
        this.deleted = true;
    }

    public boolean isOwnedBy(AppUser candidate) {
        return this.user.getId().equals(candidate.getId());
    }

    @Builder
    public Sticker(String name, String imageUrl, AppUser user) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.user = user;
    }
}
