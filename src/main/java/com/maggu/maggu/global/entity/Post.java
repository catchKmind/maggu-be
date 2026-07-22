package com.maggu.maggu.global.entity;

import com.maggu.maggu.global.entity.enums.PostCategory;
import com.maggu.maggu.user.entity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.locationtech.jts.geom.Point;

@Entity
@Table(
        name = "post",
        uniqueConstraints = @UniqueConstraint(name = "uq_post_slug", columnNames = "slug")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 20)
    private String slug;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    // 사진이 있는 글은 좌표 필수, 없으면 null 허용 — 서비스 계층에서 검증 (DB CHECK로 강제 불가)
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    // FK 아님. 관광 데이터는 적재하지 않고 이 키로 외부 API를 실시간 호출
    @Column(name = "tourism_content_id", length = 50)
    private String tourismContentId;

    @Column(name = "place_name", length = 100)
    private String placeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostCategory category;

    // 원자적 UPDATE로만 증감할 것(예: @Modifying 쿼리). 엔티티 세터로 갱신 금지 — 동시 스크랩 시 값이 유실됨.
    @Column(name = "scrap_count", nullable = false)
    private int scrapCount;

    @Builder
    public Post(AppUser user, String slug, String title, String content, Point location,
                String tourismContentId, String placeName, PostCategory category) {
        this.user = user;
        this.slug = slug;
        this.title = title;
        this.content = content;
        this.location = location;
        this.tourismContentId = tourismContentId;
        this.placeName = placeName;
        this.category = category;
        this.scrapCount = 0;
    }
}
