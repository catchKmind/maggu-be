package com.maggu.maggu.community.repository;

import com.maggu.maggu.community.entity.Post;
import com.maggu.maggu.community.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByIdAndDeletedFalse(Long id);

    Optional<Post> findBySlugAndDeletedFalse(String slug);

    // 전체 피드 - 최신순
    Page<Post> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // 전체 피드 - 스크랩 많은순(인기순)
    Page<Post> findByDeletedFalseOrderByScrapCountDescCreatedAtDesc(Pageable pageable);

    // 카테고리 탭 - 최신순
    Page<Post> findByCategoryAndDeletedFalseOrderByCreatedAtDesc(PostCategory category, Pageable pageable);

    // 카테고리 탭 - 인기순
    Page<Post> findByCategoryAndDeletedFalseOrderByScrapCountDescCreatedAtDesc(PostCategory category, Pageable pageable);

    // 키워드 검색: 본문 또는 장소명에 포함
    @Query("""
            SELECT p FROM Post p
            WHERE p.deleted = false
            AND (p.content LIKE CONCAT('%', :keyword, '%') OR p.placeName LIKE CONCAT('%', :keyword, '%'))
            ORDER BY p.createdAt DESC
            """)
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.scrapCount = p.scrapCount + 1 WHERE p.id = :postId")
    void incrementScrapCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.scrapCount = p.scrapCount - 1 WHERE p.id = :postId AND p.scrapCount > 0")
    void decrementScrapCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.reportCount = p.reportCount + 1 WHERE p.id = :postId")
    void incrementReportCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.deleted = true WHERE p.id = :postId")
    void markDeleted(@Param("postId") Long postId);
}