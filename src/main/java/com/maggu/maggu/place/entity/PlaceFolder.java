package com.maggu.maggu.place.entity;

import com.maggu.maggu.global.entity.BaseEntity;
import com.maggu.maggu.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "place_folder")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceFolder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String icon;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Builder
    public PlaceFolder(AppUser user, String name, String icon, boolean isDefault) {
        this.user = user;
        this.name = name;
        this.icon = icon;
        this.isDefault = isDefault;
    }
}
