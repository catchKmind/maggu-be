package com.maggu.maggu.community.repository;

import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.post.entity.Post;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.user = null WHERE c.user.id = :userId")
    void detachUser(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.post p
            JOIN FETCH p.user
            WHERE c.user.id = :userId
              AND c.deleted = false
              AND p.deleted = false
              AND (:cursorId IS NULL
                   OR c.createdAt < :createdAt
                   OR (c.createdAt = :createdAt AND c.id < :cursorId))
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<Comment> findPageByUserId(@Param("userId") Long userId,
                                   @Param("createdAt") Instant createdAt,
                                   @Param("cursorId") Long cursorId,
                                   Limit limit);

    long countByUserIdAndDeletedFalseAndPostDeletedFalse(Long userId);
}