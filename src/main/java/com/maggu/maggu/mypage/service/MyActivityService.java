package com.maggu.maggu.mypage.service;

import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.community.entity.PostImage;
import com.maggu.maggu.community.repository.CommentRepository;
import com.maggu.maggu.community.repository.PostImageRepository;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.global.storage.CloudFrontUrlResolver;
import com.maggu.maggu.mypage.dto.response.MyCommentListResponse;
import com.maggu.maggu.mypage.dto.response.MyPostListResponse;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.post.repository.PostRepository;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyActivityService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final CloudFrontUrlResolver cloudFrontUrlResolver;

    @Transactional(readOnly = true)
    public MyPostListResponse getMyPosts(AppUser user, String cursor, int size) {
        MyActivityCursor decodedCursor = (cursor == null) ? null : MyActivityCursor.decode(cursor);
        Instant createdAt = (decodedCursor == null) ? null : decodedCursor.createdAt();
        Long cursorId = (decodedCursor == null) ? null : decodedCursor.id();

        List<Post> fetched = postRepository.findPageByUserId(user.getId(), createdAt, cursorId, size + 1);
        CursorPageResponse<Post> page = CursorPageResponse.of(fetched, size, post -> MyActivityCursor.from(post).encode());

        List<Post> posts = page.getContent();
        Map<Long, String> thumbnailByPostId = posts.isEmpty()
                ? Map.of()
                : postImageRepository.findByPostInOrderBySortOrderAsc(posts).stream()
                .collect(Collectors.toMap(
                        image -> image.getPost().getId(),
                        PostImage::getImageUrl,
                        (first, second) -> first
                ));

        return MyPostListResponse.builder()
                .postCount(countMyPosts(user.getId()))
                .commentCount(countMyComments(user.getId()))
                .posts(page.map(post -> MyPostListResponse.MyPostResponse.builder()
                        .postId(post.getId())
                        .imageUrl(cloudFrontUrlResolver.toPublicUrl(thumbnailByPostId.get(post.getId())))
                        .content(post.getContent())
                        .build()))
                .build();
    }

    @Transactional(readOnly = true)
    public MyCommentListResponse getMyComments(AppUser user, String cursor, int size) {
        MyActivityCursor decodedCursor = (cursor == null) ? null : MyActivityCursor.decode(cursor);
        Instant createdAt = (decodedCursor == null) ? null : decodedCursor.createdAt();
        Long cursorId = (decodedCursor == null) ? null : decodedCursor.id();

        List<Comment> fetched = commentRepository.findPageByUserId(user.getId(), createdAt, cursorId, Limit.of(size + 1));
        CursorPageResponse<Comment> page = CursorPageResponse.of(fetched, size, comment -> MyActivityCursor.from(comment).encode());

        return MyCommentListResponse.builder()
                .postCount(countMyPosts(user.getId()))
                .commentCount(countMyComments(user.getId()))
                .comments(page.map(comment -> MyCommentListResponse.MyCommentResponse.builder()
                        .commentId(comment.getId())
                        .postId(comment.getPost().getId())
                        .postWriterNickname(comment.getPost().getUser().getNickname())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .isReply(comment.isReply())
                        .build()))
                .build();
    }

    private long countMyPosts(Long userId) {
        return postRepository.countByUserIdAndDeletedFalse(userId);
    }

    private long countMyComments(Long userId) {
        return commentRepository.countByUserIdAndDeletedFalseAndPostDeletedFalse(userId);
    }
}
