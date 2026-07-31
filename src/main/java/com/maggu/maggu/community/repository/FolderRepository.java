package com.maggu.maggu.community.repository;

import com.maggu.maggu.community.entity.Folder;
import com.maggu.maggu.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByUserOrderByIsDefaultDescCreatedAtAsc(AppUser user);

    Optional<Folder> findByUserAndIsDefaultTrue(AppUser user);

    boolean existsByUserAndName(AppUser user, String name);
}