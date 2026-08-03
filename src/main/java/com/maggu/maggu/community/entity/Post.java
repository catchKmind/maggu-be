<<<<<<<< HEAD:src/main/java/com/maggu/maggu/community/entity/Post.java
package com.maggu.maggu.community.entity;

import com.maggu.maggu.global.entity.BaseEntity;
========
package com.maggu.maggu.post.entity;

import com.maggu.maggu.global.entity.BaseEntity;
import com.maggu.maggu.global.entity.enums.PostCategory;
>>>>>>>> origin/main:src/main/java/com/maggu/maggu/post/entity/Post.java
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

    private static final int MAX_REPORT_COUNT = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 20)
    private String slug;

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

    @Column(name = "scrap_count", nullable = false)
    private int scrapCount;

    // 원자적 UPDATE로만 증감. 5회 이상이면 deleted=true 처리(서비스 계층 책임)
    @Column(name = "report_count", nullable = false)
    private int reportCount;

    // 신고 누적 등으로 인한 비공개 처리. 물리 삭제 대신 soft delete로 댓글/스크랩 이력 보존
    @Column(nullable = false)
    private boolean deleted;

    @Builder
    public Post(AppUser user, String slug, String content, Point location,
                String tourismContentId, String placeName, PostCategory category) {
        this.user = user;
        this.slug = slug;
        this.content = content;
        this.location = location;
        this.tourismContentId = tourismContentId;
        this.placeName = placeName;
        this.category = category;
        this.scrapCount = 0;
        this.reportCount = 0;
        this.deleted = false;
    }
<<<<<<<< HEAD:src/main/java/com/maggu/maggu/community/entity/Post.java

    public boolean isWrittenBy(AppUser candidate) {
        return this.user != null && this.user.getId().equals(candidate.getId());
    }

    public boolean shouldAutoHide() {
        return this.reportCount >= MAX_REPORT_COUNT;
    }

    public void markDeleted() {
        this.deleted = true;
    }
}
========
    // 지안아 우리 아지트에서 나가라고 해줘
}
>>>>>>>> origin/main:src/main/java/com/maggu/maggu/post/entity/Post.java
