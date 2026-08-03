package com.maggu.maggu.community.repository;

import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.community.entity.CommentLike;
import com.maggu.maggu.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentAndUser(Comment comment, AppUser user);

    boolean existsByCommentAndUser(Comment comment, AppUser user);

    // 특정 유저가 좋아요 누른 댓글 목록(응답에 likedByMe 표시용)
    List<CommentLike> findByUserAndCommentIn(AppUser user, List<Comment> comments);
}