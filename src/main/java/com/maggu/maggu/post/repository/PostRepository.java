package com.maggu.maggu.post.repository;

import com.maggu.maggu.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 지도 bbox 안의 게시글을 대표이미지와 함께 조회
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
