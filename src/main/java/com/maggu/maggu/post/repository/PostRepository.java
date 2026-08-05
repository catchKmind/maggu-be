package com.maggu.maggu.post.repository;

import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.community.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query(value = """
            SELECT p.id AS postId, p.slug AS slug,
                   ST_X(p.location) AS lng, ST_Y(p.location) AS lat,
                   p.scrap_count AS scrapCount, p.tourism_content_id AS tourismContentId,
                   p.place_name AS placeName, img.image_url AS representativeImageUrl
            FROM post p
            LEFT JOIN LATERAL (
                SELECT pi.image_url FROM post_image pi
                WHERE pi.post_id = p.id ORDER BY pi.sort_order ASC LIMIT 1
            ) img ON true
            WHERE p.location IS NOT NULL
              AND p.location && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            """, nativeQuery = true)
    List<MapPostProjection> findInBbox(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat);
}