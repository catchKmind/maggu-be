package com.maggu.maggu.community.repository;

import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.community.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 최상위 댓글만
    List<Comment> findByPostAndParentCommentIsNullAndDeletedFalseOrderByCreatedAtAsc(Post post);

    // 특정 최상위 댓글들에 달린 대댓글(1단계) 한 번에 조회 - N+1 방지
    List<Comment> findByParentCommentInAndDeletedFalseOrderByCreatedAtAsc(List<Comment> parentComments);

    long countByPostAndDeletedFalse(Post post);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :commentId")
    void incrementLikeCount(@Param("commentId") Long commentId);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :commentId AND c.likeCount > 0")
    void decrementLikeCount(@Param("commentId") Long commentId);

    @Modifying
    @Query("UPDATE Comment c SET c.reportCount = c.reportCount + 1 WHERE c.id = :commentId")
    void incrementReportCount(@Param("commentId") Long commentId);

    @Modifying
    @Query("UPDATE Comment c SET c.deleted = true WHERE c.id = :commentId")
    void markDeleted(@Param("commentId") Long commentId);
}