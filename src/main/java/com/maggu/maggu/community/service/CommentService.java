package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.request.CommentCreateRequest;
import com.maggu.maggu.community.dto.response.CommentCreateResponse;
import com.maggu.maggu.community.dto.response.CommentLikeResponse;
import com.maggu.maggu.community.dto.response.CommentResponse;
import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.community.entity.CommentLike;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.community.repository.CommentLikeRepository;
import com.maggu.maggu.community.repository.CommentRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final String WITHDRAWN_USER_LABEL = "탈퇴한 회원";

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostQueryService postQueryService;

    @Transactional
    public CommentCreateResponse createComment(AppUser writer, Long postId, CommentCreateRequest request) {
        Post post = postQueryService.getActivePost(postId);

        Comment parent = null;
        if (request.getParentCommentId() != null) {
            parent = commentRepository.findById(request.getParentCommentId())
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
            if (parent.isReply()) {
                // 대댓글의 대댓글 금지 - 1단계까지만 허용
                throw new BusinessException(ErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED);
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .user(writer)
                .parentComment(parent)
                .content(request.getContent())
                .build();
        commentRepository.save(comment);

        return CommentCreateResponse.builder()
                .commentId(comment.getId())
                .parentCommentId(parent != null ? parent.getId() : null)
                .build();
    }

    public List<CommentResponse> getComments(Long postId, AppUser viewer) {
        Post post = postQueryService.getActivePost(postId);

        List<Comment> topLevel = commentRepository
                .findByPostAndParentCommentIsNullAndDeletedFalseOrderByCreatedAtAsc(post);
        if (topLevel.isEmpty()) {
            return List.of();
        }

        List<Comment> replies = commentRepository.findByParentCommentInAndDeletedFalseOrderByCreatedAtAsc(topLevel);

        List<Comment> all = new ArrayList<>(topLevel);
        all.addAll(replies);
        Set<Long> likedCommentIds = commentLikeRepository.findByUserAndCommentIn(viewer, all).stream()
                .map(like -> like.getComment().getId())
                .collect(Collectors.toSet());

        Map<Long, List<CommentResponse>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(
                        reply -> reply.getParentComment().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                reply -> toResponse(reply, likedCommentIds.contains(reply.getId()), List.of()),
                                Collectors.toList())
                ));

        return topLevel.stream()
                .map(comment -> toResponse(
                        comment,
                        likedCommentIds.contains(comment.getId()),
                        repliesByParentId.getOrDefault(comment.getId(), List.of())))
                .toList();
    }

    // 토글 방식: 이미 눌렀으면 취소, 안 눌렀으면 공감
    @Transactional
    public CommentLikeResponse toggleLike(AppUser user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        Optional<CommentLike> existing = commentLikeRepository.findByCommentAndUser(comment, user);
        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
            commentRepository.decrementLikeCount(commentId);
            return CommentLikeResponse.builder().commentId(commentId).liked(false).build();
        }

        commentLikeRepository.save(CommentLike.builder().comment(comment).user(user).build());
        commentRepository.incrementLikeCount(commentId);
        return CommentLikeResponse.builder().commentId(commentId).liked(true).build();
    }

    private CommentResponse toResponse(Comment comment, boolean likedByMe, List<CommentResponse> replies) {
        String nickname = comment.getUser() != null ? comment.getUser().getNickname() : WITHDRAWN_USER_LABEL;

        boolean isPostWriter = comment.getPost() != null && comment.getPost().isWrittenBy(comment.getUser());

        return CommentResponse.builder()
                .commentId(comment.getId())
                .writerNickname(nickname)
                .isPostWriter(isPostWriter)
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .likedByMe(likedByMe)
                .createdAt(comment.getCreatedAt())
                .replies(replies)
                .build();
    }
}