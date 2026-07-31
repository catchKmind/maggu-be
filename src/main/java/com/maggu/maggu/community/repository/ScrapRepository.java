package com.maggu.maggu.community.repository;

import com.maggu.maggu.community.entity.Folder;
import com.maggu.maggu.community.entity.Post;
import com.maggu.maggu.community.entity.Scrap;
import com.maggu.maggu.user.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    boolean existsByUserAndPost(AppUser user, Post post);

    Optional<Scrap> findByUserAndPost(AppUser user, Post post);

    Page<Scrap> findByUserAndFolderOrderByCreatedAtDesc(AppUser user, Folder folder, Pageable pageable);

    // 상세/피드 응답에서 "내가 스크랩했는지" 표시용
    List<Scrap> findByUserAndPostIn(AppUser user, List<Post> posts);
}