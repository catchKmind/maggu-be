package com.maggu.maggu.community.repository;

import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.community.entity.PostStickerReaction;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostStickerReactionRepository extends JpaRepository<PostStickerReaction, Long> {

    Optional<PostStickerReaction> findByPostAndUser(Post post, AppUser user);

    @Query("""
            SELECT r.sticker AS sticker, COUNT(r) AS count
            FROM PostStickerReaction r
            WHERE r.post = :post
            GROUP BY r.sticker
            """)
    List<StickerCount> countByPostGroupBySticker(@Param("post") Post post);

    // 피드 목록 게시글들의 스티커 반응 총 개수를 한 번에 조회
    @Query("""
            SELECT r.post.id AS postId, COUNT(r) AS count
            FROM PostStickerReaction r
            WHERE r.post IN :posts
            GROUP BY r.post.id
            """)
    List<PostReactionCount> countByPostInGrouped(@Param("posts") List<Post> posts);

    // 피드 목록 중 내가 반응한 내역 한 번에 조회
    List<PostStickerReaction> findByUserAndPostIn(AppUser user, List<Post> posts);

    interface StickerCount {
        Sticker getSticker();

        long getCount();
    }

    interface PostReactionCount {
        Long getPostId();

        long getCount();
    }
}