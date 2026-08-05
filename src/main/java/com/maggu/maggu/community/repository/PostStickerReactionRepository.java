package com.maggu.maggu.community.repository;

import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.community.entity.PostStickerReaction;
import com.maggu.maggu.community.entity.Sticker;
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

    interface StickerCount {
        Sticker getSticker();
        long getCount();
    }
}