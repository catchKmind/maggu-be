package com.maggu.maggu.global.entity;

import com.maggu.maggu.user.entity.AppUser;
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

// 폴더 이동(UPDATE scrap SET folder_id) 시나리오가 있어 BaseEntity(createdAt+updatedAt) 상속
@Entity
@Table(
        name = "scrap",
        uniqueConstraints = @UniqueConstraint(name = "uq_scrap_user_post", columnNames = {"user_id", "post_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scrap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @Builder
    public Scrap(AppUser user, Post post, Folder folder) {
        this.user = user;
        this.post = post;
        this.folder = folder;
    }

    // 폴더 이동은 DELETE+INSERT가 아니라 이 필드를 UPDATE하는 방식으로 처리(scrap_count 불변 유지)
    public void changeFolder(Folder folder) {
        this.folder = folder;
    }
}
