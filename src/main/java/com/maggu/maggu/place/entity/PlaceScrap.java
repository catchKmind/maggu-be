package com.maggu.maggu.place.entity;

import com.maggu.maggu.global.entity.BaseEntity;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "place_scrap",
        indexes = @Index(name = "idx_place_scrap_folder", columnList = "place_folder_id"),
        uniqueConstraints = @UniqueConstraint(name = "uq_place_scrap_folder_spot",
                columnNames = {"place_folder_id", "tourism_content_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceScrap extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "tourism_content_id", nullable = false, length = 50)
    private String tourismContentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sticker_id", nullable = false)
    private Sticker sticker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_folder_id", nullable = false)
    private PlaceFolder placeFolder;

    @Builder
    public PlaceScrap(AppUser user, String tourismContentId, Sticker sticker, PlaceFolder placeFolder) {
        this.user = user;
        this.tourismContentId = tourismContentId;
        this.sticker = sticker;
        this.placeFolder = placeFolder;
    }
}
