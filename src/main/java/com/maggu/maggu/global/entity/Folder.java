package com.maggu.maggu.global.entity;

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
@Table(name = "folder")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Folder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 50)
    private String name;

    // 유저당 기본 폴더는 1개(부분 유니크 인덱스 uq_folder_one_default, ddl-auto로 자동 생성 안 됨 — 수동 반영 필요).
    // 기본 폴더 삭제 방지는 DB 트리거가 아닌 서비스 계층 책임.
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Builder
    public Folder(AppUser user, String name, boolean isDefault) {
        this.user = user;
        this.name = name;
        this.isDefault = isDefault;
    }
}
