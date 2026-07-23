package com.maggu.maggu.user.repository;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByNickname(String nickname);

    Optional<AppUser> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}
