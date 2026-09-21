package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.request.PostCreateRequest;
import com.maggu.maggu.community.dto.response.PostCreateResponse;
import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.community.entity.PostImage;
import com.maggu.maggu.community.repository.PostImageRepository;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.post.repository.PostRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostCommandServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostQueryService postQueryService;

    @InjectMocks
    private PostCommandService postCommandService;

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("본문만 있으면 제목은 본문에서 채우고 위치 없이 저장한다")
        void createsTextOnlyPostWithTitleFromContent() {
            AppUser writer = appUser(1L);
            stubSave();

            PostCreateResponse result = postCommandService.createPost(writer,
                    new PostCreateRequest("해운대 다녀왔어요", PostCategory.RECOMMEND,
                            null, null, null, null, null, null));

            assertThat(result.getPostId()).isEqualTo(10L);
            assertThat(result.getSlug()).isNotBlank();

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("해운대 다녀왔어요");
            assertThat(captor.getValue().getContent()).isEqualTo("해운대 다녀왔어요");
            assertThat(captor.getValue().getLocation()).isNull();
            verify(postImageRepository, never()).save(any());
        }

        @Test
        @DisplayName("스웨거 기본 좌표 (0,0)은 위치로 저장하지 않는다")
        void ignoresZeroCoordinatesWhenNoImages() {
            AppUser writer = appUser(1L);
            stubSave();

            postCommandService.createPost(writer,
                    new PostCreateRequest("본문", PostCategory.INFO,
                            List.of(), "string", "string", PostCreateRequest.LocationSource.AUTO, 0.0, 0.0));

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getLocation()).isNull();
        }

        @Test
        @DisplayName("사진과 유효한 좌표가 있으면 위치와 이미지를 저장한다")
        void createsPostWithImagesAndLocation() {
            AppUser writer = appUser(1L);
            stubSave();

            postCommandService.createPost(writer,
                    new PostCreateRequest("본문", PostCategory.RECOMMEND,
                            List.of("https://img/a.jpg"), "해운대", "126234",
                            PostCreateRequest.LocationSource.MANUAL, 35.16, 129.16));

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getLocation()).isNotNull();
            assertThat(captor.getValue().getLocation().getY()).isEqualTo(35.16);
            assertThat(captor.getValue().getLocation().getX()).isEqualTo(129.16);
            verify(postImageRepository).save(any(PostImage.class));
        }

        @Test
        @DisplayName("사진이 있는데 좌표가 없으면 예외를 던진다")
        void throwsWhenImagesExistWithoutLocation() {
            AppUser writer = appUser(1L);

            assertThatThrownBy(() -> postCommandService.createPost(writer,
                    new PostCreateRequest("본문", PostCategory.RECOMMEND,
                            List.of("https://img/a.jpg"), null, null, null, null, null)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_LOCATION_REQUIRED));

            verify(postRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("사진이 있는데 좌표가 (0,0)이면 예외를 던진다")
        void throwsWhenImagesExistWithZeroCoordinates() {
            AppUser writer = appUser(1L);

            assertThatThrownBy(() -> postCommandService.createPost(writer,
                    new PostCreateRequest("본문", PostCategory.RECOMMEND,
                            List.of("https://img/a.jpg"), null, null, null, 0.0, 0.0)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.POST_LOCATION_REQUIRED));
        }

        @Test
        @DisplayName("빈 이미지 URL은 무시하고 사진 없는 글로 저장한다")
        void ignoresBlankImageUrls() {
            AppUser writer = appUser(1L);
            stubSave();

            postCommandService.createPost(writer,
                    new PostCreateRequest("본문", PostCategory.RECOMMEND,
                            List.of(" ", ""), null, null, null, null, null));

            verify(postImageRepository, never()).save(any());
        }

        @Test
        @DisplayName("본문이 100자를 넘으면 제목은 100자로 자른다")
        void truncatesTitleToMaxLength() {
            AppUser writer = appUser(1L);
            stubSave();
            String content = "가".repeat(120);

            postCommandService.createPost(writer,
                    new PostCreateRequest(content, PostCategory.CURATION,
                            null, null, null, null, null, null));

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getTitle()).hasSize(100);
        }
    }

    private void stubSave() {
        given(postRepository.saveAndFlush(any(Post.class))).willAnswer(invocation -> {
            Post toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", 10L);
            return toSave;
        });
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
