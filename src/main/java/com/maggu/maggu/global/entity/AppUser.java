package com.maggu.maggu.global.entity;

import com.maggu.maggu.global.entity.enums.Provider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "app_user",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_provider", columnNames = {"provider", "provider_user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 30, unique = true)
    private String nickname;

    @Builder
    public AppUser(Provider provider, String providerUserId, String email, String nickname) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.nickname = nickname;
    }
}
