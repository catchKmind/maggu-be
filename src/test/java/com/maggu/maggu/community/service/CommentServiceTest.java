package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.request.CommentCreateRequest;
import com.maggu.maggu.community.dto.response.CommentCreateResponse;
import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.community.repository.CommentLikeRepository;
import com.maggu.maggu.community.repository.CommentRepository;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private PostQueryService postQueryService;

    @InjectMocks
    private CommentService commentService;

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("parentCommentId가 없으면 원댓글로 저장한다")
        void createsRootComment() {
            AppUser writer = appUser(1L);
            Post post = post(10L, writer);
            given(postQueryService.getActivePost(10L)).willReturn(post);
            stubSave(30L);

            CommentCreateResponse result = commentService.createComment(writer, 10L, request("첫 댓글", null));

            assertThat(result.getCommentId()).isEqualTo(30L);
            assertThat(result.getParentCommentId()).isNull();

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertThat(captor.getValue().getParentComment()).isNull();
        }

        @Test
        @DisplayName("원댓글 ID를 보내면 그 원댓글 아래 대댓글로 저장한다")
        void createsReplyUnderRoot() {
            AppUser writer = appUser(2L);
            Post post = post(10L, appUser(1L));
            Comment root = comment(1L, post, appUser(1L), null, "원댓글");
            given(postQueryService.getActivePost(10L)).willReturn(post);
            given(commentRepository.findById(1L)).willReturn(Optional.of(root));
            stubSave(31L);

            CommentCreateResponse result = commentService.createComment(writer, 10L, request("대댓글", 1L));

            assertThat(result.getParentCommentId()).isEqualTo(1L);
            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertThat(captor.getValue().getParentComment().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("대댓글 ID를 보내도 원댓글 아래로 귀속한다")
        void flattensReplyToReplyOntoRoot() {
            AppUser writer = appUser(3L);
            Post post = post(10L, appUser(1L));
            Comment root = comment(1L, post, appUser(1L), null, "원댓글");
            Comment reply = comment(2L, post, appUser(2L), root, "대댓글");
            given(postQueryService.getActivePost(10L)).willReturn(post);
            given(commentRepository.findById(2L)).willReturn(Optional.of(reply));
            stubSave(32L);

            CommentCreateResponse result = commentService.createComment(writer, 10L, request("대대댓글처럼 보여도", 2L));

            assertThat(result.getParentCommentId()).isEqualTo(1L);
            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertThat(captor.getValue().getParentComment().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("다른 게시글 댓글을 parent로 보내면 찾을 수 없다")
        void rejectsParentFromOtherPost() {
            AppUser writer = appUser(1L);
            Post post = post(10L, writer);
            Post otherPost = post(99L, writer);
            Comment otherRoot = comment(7L, otherPost, writer, null, "다른 글 댓글");
            given(postQueryService.getActivePost(10L)).willReturn(post);
            given(commentRepository.findById(7L)).willReturn(Optional.of(otherRoot));

            assertThatThrownBy(() -> commentService.createComment(writer, 10L, request("댓글", 7L)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    private void stubSave(Long id) {
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", id);
            return toSave;
        });
    }

    private CommentCreateRequest request(String content, Long parentCommentId) {
        CommentCreateRequest request = new CommentCreateRequest();
        ReflectionTestUtils.setField(request, "content", content);
        ReflectionTestUtils.setField(request, "parentCommentId", parentCommentId);
        return request;
    }

    private Comment comment(Long id, Post post, AppUser writer, Comment parent, String content) {
        Comment comment = Comment.builder()
                .post(post)
                .user(writer)
                .parentComment(parent)
                .content(content)
                .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private Post post(Long id, AppUser writer) {
        Post post = Post.builder()
                .user(writer)
                .slug("slug" + id)
                .title("제목")
                .content("본문")
                .category(PostCategory.RECOMMEND)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private AppUser appUser(Long id) {
        AppUser user = AppUser.builder()
                .provider(Provider.APPLE)
                .providerUserId("apple-" + id)
                .email("user" + id + "@test.com")
                .nickname("유저" + id)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
