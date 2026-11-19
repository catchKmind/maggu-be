package com.maggu.maggu.mypage.service;

import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.community.entity.PostImage;
import com.maggu.maggu.community.repository.CommentRepository;
import com.maggu.maggu.community.repository.PostImageRepository;
import com.maggu.maggu.global.config.CloudFrontProperties;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.storage.CloudFrontUrlResolver;
import com.maggu.maggu.mypage.dto.response.MyCommentListResponse;
import com.maggu.maggu.mypage.dto.response.MyCommentListResponse.MyCommentResponse;
import com.maggu.maggu.mypage.dto.response.MyPostListResponse;
import com.maggu.maggu.mypage.dto.response.MyPostListResponse.MyPostResponse;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.post.repository.PostRepository;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MyActivityServiceTest {

    private static final int SIZE = 2;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private CommentRepository commentRepository;

    @Spy
    private CloudFrontUrlResolver cloudFrontUrlResolver =
            new CloudFrontUrlResolver(new CloudFrontProperties(""), "test-bucket");

    @InjectMocks
    private MyActivityService myActivityService;

    @Nested
    @DisplayName("getMyPosts")
    class GetMyPosts {

        @Test
        @DisplayName("size+1개가 조회되면 size개만 담고 hasNext=true, 마지막 항목으로 nextCursor를 만든다")
        void returnsFirstPageWithNextCursorWhenMoreExists() {
            AppUser user = appUser(1L, "나그네");
            Post first = post(103L, user, "세 번째 글", Instant.parse("2026-11-03T00:00:00Z"));
            Post second = post(102L, user, "두 번째 글", Instant.parse("2026-11-02T00:00:00Z"));
            Post extra = post(101L, user, "첫 번째 글", Instant.parse("2026-11-01T00:00:00Z"));
            given(postRepository.findPageByUserId(1L, null, null, SIZE + 1)).willReturn(List.of(first, second, extra));

            MyPostListResponse result = myActivityService.getMyPosts(user, null, SIZE);

            assertThat(result.posts().getContent()).extracting(MyPostResponse::postId).containsExactly(103L, 102L);
            assertThat(result.posts().isHasNext()).isTrue();
            assertThat(result.posts().getNextCursor()).isEqualTo(MyActivityCursor.from(second).encode());
        }

        @Test
        @DisplayName("size개 이하로 조회되면 마지막 페이지라 hasNext=false, nextCursor는 null이다")
        void returnsLastPageWithoutNextCursor() {
            AppUser user = appUser(1L, "나그네");
            Post only = post(101L, user, "첫 번째 글", Instant.parse("2026-11-01T00:00:00Z"));
            given(postRepository.findPageByUserId(1L, null, null, SIZE + 1)).willReturn(List.of(only));

            MyPostListResponse result = myActivityService.getMyPosts(user, null, SIZE);

            assertThat(result.posts().getContent()).hasSize(1);
            assertThat(result.posts().isHasNext()).isFalse();
            assertThat(result.posts().getNextCursor()).isNull();
        }

        @Test
        @DisplayName("커서가 있으면 디코딩한 createdAt/id를 리포지토리에 넘긴다")
        void passesDecodedCursorToRepository() {
            AppUser user = appUser(1L, "나그네");
            Instant cursorCreatedAt = Instant.parse("2026-11-02T00:00:00Z");
            String cursor = new MyActivityCursor(cursorCreatedAt, 102L).encode();
            given(postRepository.findPageByUserId(1L, cursorCreatedAt, 102L, SIZE + 1)).willReturn(List.of());

            myActivityService.getMyPosts(user, cursor, SIZE);

            verify(postRepository).findPageByUserId(1L, cursorCreatedAt, 102L, SIZE + 1);
        }

        @Test
        @DisplayName("잘못된 커서면 INVALID_INPUT_VALUE 예외를 던지고 조회하지 않는다")
        void throwsWhenCursorInvalid() {
            AppUser user = appUser(1L, "나그네");

            assertThatThrownBy(() -> myActivityService.getMyPosts(user, "broken!!", SIZE))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository, postImageRepository, commentRepository);
        }

        @Test
        @DisplayName("대표 이미지는 첫 번째 이미지를 쓰고, 사진 없는 글은 imageUrl이 null이다")
        void assemblesThumbnailAndContent() {
            AppUser user = appUser(1L, "나그네");
            Post withImages = post(102L, user, "사진 있는 글", Instant.parse("2026-11-02T00:00:00Z"));
            Post withoutImage = post(101L, user, "사진 없는 글", Instant.parse("2026-11-01T00:00:00Z"));
            given(postRepository.findPageByUserId(1L, null, null, SIZE + 1)).willReturn(List.of(withImages, withoutImage));
            given(postImageRepository.findByPostInOrderBySortOrderAsc(List.of(withImages, withoutImage))).willReturn(List.of(
                    postImage(withImages, "POST/1/first.png", 0),
                    postImage(withImages, "POST/1/second.png", 1)
            ));

            MyPostListResponse result = myActivityService.getMyPosts(user, null, SIZE);

            assertThat(result.posts().getContent())
                    .extracting(MyPostResponse::postId, MyPostResponse::imageUrl, MyPostResponse::content)
                    .containsExactly(
                            tuple(102L, "POST/1/first.png", "사진 있는 글"),
                            tuple(101L, null, "사진 없는 글")
                    );
        }

        @Test
        @DisplayName("확인용으로 더 가져온 1건은 이미지 조회 대상에서 제외한다")
        void doesNotLookUpImagesForExtraFetchedItem() {
            AppUser user = appUser(1L, "나그네");
            Post first = post(103L, user, "세 번째 글", Instant.parse("2026-11-03T00:00:00Z"));
            Post second = post(102L, user, "두 번째 글", Instant.parse("2026-11-02T00:00:00Z"));
            Post extra = post(101L, user, "첫 번째 글", Instant.parse("2026-11-01T00:00:00Z"));
            given(postRepository.findPageByUserId(1L, null, null, SIZE + 1)).willReturn(List.of(first, second, extra));

            myActivityService.getMyPosts(user, null, SIZE);

            verify(postImageRepository).findByPostInOrderBySortOrderAsc(List.of(first, second));
        }

        @Test
        @DisplayName("작성한 글이 없으면 이미지 조회 없이 빈 페이지를 반환한다")
        void skipsImageLookUpWhenEmpty() {
            AppUser user = appUser(1L, "나그네");
            given(postRepository.findPageByUserId(1L, null, null, SIZE + 1)).willReturn(List.of());

            MyPostListResponse result = myActivityService.getMyPosts(user, null, SIZE);

            assertThat(result.posts().getContent()).isEmpty();
            assertThat(result.posts().isHasNext()).isFalse();
            verify(postImageRepository, never()).findByPostInOrderBySortOrderAsc(anyList());
        }

        @Test
        @DisplayName("글 수/댓글 수는 현재 페이지 크기가 아니라 전체 개수다")
        void countsAreWholeCounts() {
            AppUser user = appUser(1L, "나그네");
            given(postRepository.findPageByUserId(1L, null, null, SIZE + 1)).willReturn(List.of());
            given(postRepository.countByUserIdAndDeletedFalse(1L)).willReturn(57L);
            given(commentRepository.countByUserIdAndDeletedFalseAndPostDeletedFalse(1L)).willReturn(12L);

            MyPostListResponse result = myActivityService.getMyPosts(user, null, SIZE);

            assertThat(result.postCount()).isEqualTo(57);
            assertThat(result.commentCount()).isEqualTo(12);
        }
    }

    @Nested
    @DisplayName("getMyComments")
    class GetMyComments {

        @Test
        @DisplayName("size+1개가 조회되면 size개만 담고 hasNext=true, 마지막 항목으로 nextCursor를 만든다")
        void returnsFirstPageWithNextCursorWhenMoreExists() {
            AppUser me = appUser(1L, "나그네");
            Post post = post(10L, appUser(2L, "작성자"), "원본 글", Instant.parse("2026-11-01T00:00:00Z"));
            Comment first = comment(203L, post, me, null, "세 번째 댓글", Instant.parse("2026-11-03T00:00:00Z"));
            Comment second = comment(202L, post, me, null, "두 번째 댓글", Instant.parse("2026-11-02T00:00:00Z"));
            Comment extra = comment(201L, post, me, null, "첫 번째 댓글", Instant.parse("2026-11-01T00:00:00Z"));
            given(commentRepository.findPageByUserId(1L, null, null, Limit.of(SIZE + 1))).willReturn(List.of(first, second, extra));

            MyCommentListResponse result = myActivityService.getMyComments(me, null, SIZE);

            assertThat(result.comments().getContent()).extracting(MyCommentResponse::commentId).containsExactly(203L, 202L);
            assertThat(result.comments().isHasNext()).isTrue();
            assertThat(result.comments().getNextCursor()).isEqualTo(MyActivityCursor.from(second).encode());
        }

        @Test
        @DisplayName("size개 이하로 조회되면 마지막 페이지라 hasNext=false, nextCursor는 null이다")
        void returnsLastPageWithoutNextCursor() {
            AppUser me = appUser(1L, "나그네");
            Post post = post(10L, appUser(2L, "작성자"), "원본 글", Instant.parse("2026-11-01T00:00:00Z"));
            Comment only = comment(201L, post, me, null, "댓글", Instant.parse("2026-11-01T00:00:00Z"));
            given(commentRepository.findPageByUserId(1L, null, null, Limit.of(SIZE + 1))).willReturn(List.of(only));

            MyCommentListResponse result = myActivityService.getMyComments(me, null, SIZE);

            assertThat(result.comments().getContent()).hasSize(1);
            assertThat(result.comments().isHasNext()).isFalse();
            assertThat(result.comments().getNextCursor()).isNull();
        }

        @Test
        @DisplayName("커서가 있으면 디코딩한 createdAt/id와 size+1 Limit을 리포지토리에 넘긴다")
        void passesDecodedCursorToRepository() {
            AppUser me = appUser(1L, "나그네");
            Instant cursorCreatedAt = Instant.parse("2026-11-02T00:00:00Z");
            String cursor = new MyActivityCursor(cursorCreatedAt, 202L).encode();
            given(commentRepository.findPageByUserId(1L, cursorCreatedAt, 202L, Limit.of(SIZE + 1))).willReturn(List.of());

            myActivityService.getMyComments(me, cursor, SIZE);

            verify(commentRepository).findPageByUserId(1L, cursorCreatedAt, 202L, Limit.of(SIZE + 1));
        }

        @Test
        @DisplayName("잘못된 커서면 INVALID_INPUT_VALUE 예외를 던지고 조회하지 않는다")
        void throwsWhenCursorInvalid() {
            AppUser me = appUser(1L, "나그네");

            assertThatThrownBy(() -> myActivityService.getMyComments(me, "broken!!", SIZE))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            verifyNoInteractions(postRepository, postImageRepository, commentRepository);
        }

        @Test
        @DisplayName("원본 게시글 ID/작성자 닉네임, 댓글 내용/작성일, 답글 여부를 응답으로 조립한다")
        void assemblesCommentFields() {
            AppUser me = appUser(1L, "나그네");
            Post post = post(10L, appUser(2L, "원글작성자"), "원본 글", Instant.parse("2026-11-01T00:00:00Z"));
            Instant parentCreatedAt = Instant.parse("2026-11-02T00:00:00Z");
            Instant replyCreatedAt = Instant.parse("2026-11-03T00:00:00Z");
            Comment parent = comment(201L, post, me, null, "최상위 댓글", parentCreatedAt);
            Comment reply = comment(202L, post, me, parent, "답글", replyCreatedAt);
            given(commentRepository.findPageByUserId(1L, null, null, Limit.of(SIZE + 1))).willReturn(List.of(reply, parent));

            MyCommentListResponse result = myActivityService.getMyComments(me, null, SIZE);

            assertThat(result.comments().getContent())
                    .extracting(MyCommentResponse::commentId, MyCommentResponse::postId, MyCommentResponse::postWriterNickname,
                            MyCommentResponse::content, MyCommentResponse::createdAt, MyCommentResponse::isReply)
                    .containsExactly(
                            tuple(202L, 10L, "원글작성자", "답글", replyCreatedAt, true),
                            tuple(201L, 10L, "원글작성자", "최상위 댓글", parentCreatedAt, false)
                    );
        }

        @Test
        @DisplayName("작성한 댓글이 없으면 빈 페이지를 반환한다")
        void returnsEmptyPageWhenNoComments() {
            AppUser me = appUser(1L, "나그네");
            given(commentRepository.findPageByUserId(any(), any(), any(), any())).willReturn(List.of());

            MyCommentListResponse result = myActivityService.getMyComments(me, null, SIZE);

            assertThat(result.comments().getContent()).isEmpty();
            assertThat(result.comments().isHasNext()).isFalse();
            assertThat(result.comments().getNextCursor()).isNull();
        }

        @Test
        @DisplayName("글 수/댓글 수는 현재 페이지 크기가 아니라 전체 개수다")
        void countsAreWholeCounts() {
            AppUser me = appUser(1L, "나그네");
            given(commentRepository.findPageByUserId(1L, null, null, Limit.of(SIZE + 1))).willReturn(List.of());
            given(postRepository.countByUserIdAndDeletedFalse(1L)).willReturn(3L);
            given(commentRepository.countByUserIdAndDeletedFalseAndPostDeletedFalse(1L)).willReturn(41L);

            MyCommentListResponse result = myActivityService.getMyComments(me, null, SIZE);

            assertThat(result.postCount()).isEqualTo(3);
            assertThat(result.commentCount()).isEqualTo(41);
            verify(postRepository, never()).findPageByUserId(any(), any(), any(), anyInt());
        }
    }

    private AppUser appUser(Long id, String nickname) {
        AppUser user = AppUser.builder()
                .provider(Provider.TEST)
                .providerUserId("provider-user-" + id)
                .email("user" + id + "@maggu.com")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Post post(Long id, AppUser writer, String content, Instant createdAt) {
        Post post = Post.builder()
                .user(writer)
                .content(content)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        return post;
    }

    private PostImage postImage(Post post, String imageUrl, int sortOrder) {
        return PostImage.builder()
                .post(post)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();
    }

    private Comment comment(Long id, Post post, AppUser writer, Comment parent, String content, Instant createdAt) {
        Comment comment = Comment.builder()
                .post(post)
                .user(writer)
                .parentComment(parent)
                .content(content)
                .build();
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "createdAt", createdAt);
        return comment;
    }
}
