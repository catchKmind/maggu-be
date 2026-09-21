package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.request.ScrapCreateRequest;
import com.maggu.maggu.community.dto.response.FolderResponse;
import com.maggu.maggu.community.dto.response.PageResponse;
import com.maggu.maggu.community.dto.response.PostSummaryResponse;
import com.maggu.maggu.community.dto.response.ScrapResponse;
import com.maggu.maggu.community.entity.Folder;
import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.community.entity.Scrap;
import com.maggu.maggu.community.repository.FolderRepository;
import com.maggu.maggu.community.repository.ScrapRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScrapServiceTest {

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostQueryService postQueryService;

    @InjectMocks
    private ScrapService scrapService;

    @Nested
    @DisplayName("scrap")
    class ScrapPost {

        @Test
        @DisplayName("folderId가 없으면 기본 폴더에 저장한다")
        void scrapsIntoDefaultFolderWhenFolderIdMissing() {
            AppUser user = appUser(1L);
            Post post = post(10L, user);
            Folder defaultFolder = folder(3L, user, "기본 폴더", true);
            given(postRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
            given(scrapRepository.existsByUserAndPost(user, post)).willReturn(false);
            given(folderRepository.findByUserAndIsDefaultTrue(user)).willReturn(Optional.of(defaultFolder));
            given(scrapRepository.saveAndFlush(any(Scrap.class))).willAnswer(invocation -> invocation.getArgument(0));

            ScrapResponse result = scrapService.scrap(user, new ScrapCreateRequest(10L, null));

            assertThat(result.getPostId()).isEqualTo(10L);
            assertThat(result.getFolderId()).isEqualTo(3L);
            assertThat(result.isScrapped()).isTrue();
            verify(postRepository).incrementScrapCount(10L);
        }

        @Test
        @DisplayName("기본 폴더가 없으면 만들고 그 폴더에 저장한다")
        void createsDefaultFolderWhenMissing() {
            AppUser user = appUser(1L);
            Post post = post(10L, user);
            Folder created = folder(7L, user, "기본 폴더", true);
            given(postRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
            given(scrapRepository.existsByUserAndPost(user, post)).willReturn(false);
            given(folderRepository.findByUserAndIsDefaultTrue(user)).willReturn(Optional.empty());
            given(folderRepository.save(any(Folder.class))).willReturn(created);
            given(scrapRepository.saveAndFlush(any(Scrap.class))).willAnswer(invocation -> invocation.getArgument(0));

            ScrapResponse result = scrapService.scrap(user, new ScrapCreateRequest(10L, 0L));

            assertThat(result.getFolderId()).isEqualTo(7L);
            ArgumentCaptor<Folder> captor = ArgumentCaptor.forClass(Folder.class);
            verify(folderRepository).save(captor.capture());
            assertThat(captor.getValue().isDefault()).isTrue();
        }

        @Test
        @DisplayName("이미 스크랩한 게시글이면 예외를 던지고 저장하지 않는다")
        void throwsWhenAlreadyScrapped() {
            AppUser user = appUser(1L);
            Post post = post(10L, user);
            given(postRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
            given(scrapRepository.existsByUserAndPost(user, post)).willReturn(true);

            assertThatThrownBy(() -> scrapService.scrap(user, new ScrapCreateRequest(10L, null)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCRAP_DUPLICATE));

            verify(scrapRepository, never()).saveAndFlush(any());
            verify(postRepository, never()).incrementScrapCount(any());
        }

        @Test
        @DisplayName("동시 스크랩으로 유니크 제약이 깨지면 중복 예외로 변환한다")
        void convertsUniqueConstraintToDuplicate() {
            AppUser user = appUser(1L);
            Post post = post(10L, user);
            Folder defaultFolder = folder(3L, user, "기본 폴더", true);
            given(postRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
            given(scrapRepository.existsByUserAndPost(user, post)).willReturn(false);
            given(folderRepository.findByUserAndIsDefaultTrue(user)).willReturn(Optional.of(defaultFolder));
            willThrow(new DataIntegrityViolationException("duplicate"))
                    .given(scrapRepository).saveAndFlush(any(Scrap.class));

            assertThatThrownBy(() -> scrapService.scrap(user, new ScrapCreateRequest(10L, null)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCRAP_DUPLICATE));
        }

        @Test
        @DisplayName("다른 유저 폴더면 접근을 거부한다")
        void throwsWhenFolderNotOwned() {
            AppUser user = appUser(1L);
            AppUser other = appUser(2L);
            Post post = post(10L, user);
            Folder othersFolder = folder(9L, other, "남의 폴더", false);
            given(postRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
            given(scrapRepository.existsByUserAndPost(user, post)).willReturn(false);
            given(folderRepository.findById(9L)).willReturn(Optional.of(othersFolder));

            assertThatThrownBy(() -> scrapService.scrap(user, new ScrapCreateRequest(10L, 9L)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FOLDER_ACCESS_DENIED));
        }
    }

    @Nested
    @DisplayName("getFolders")
    class GetFolders {

        @Test
        @DisplayName("기본 폴더가 없으면 만들고 목록을 반환한다")
        void createsDefaultFolderWhenListing() {
            AppUser user = appUser(1L);
            Folder defaultFolder = folder(3L, user, "기본 폴더", true);
            given(folderRepository.findByUserAndIsDefaultTrue(user)).willReturn(Optional.empty());
            given(folderRepository.save(any(Folder.class))).willReturn(defaultFolder);
            given(folderRepository.findByUserOrderByIsDefaultDescCreatedAtAsc(user))
                    .willReturn(List.of(defaultFolder));

            List<FolderResponse> result = scrapService.getFolders(user);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).folderId()).isEqualTo(3L);
            assertThat(result.get(0).isDefault()).isTrue();
        }
    }

    @Nested
    @DisplayName("getScrapsInFolder")
    class GetScrapsInFolder {

        @Test
        @DisplayName("삭제되지 않은 게시글만 피드와 같은 요약 응답으로 반환한다")
        void returnsSummariesForActivePosts() {
            AppUser user = appUser(1L);
            Folder folder = folder(3L, user, "기본 폴더", true);
            Post post = post(10L, user);
            Scrap scrap = Scrap.builder().user(user).post(post).folder(folder).build();
            Page<Scrap> scrapPage = new PageImpl<>(List.of(scrap));
            PageResponse<PostSummaryResponse> summaries = PageResponse.<PostSummaryResponse>builder()
                    .content(List.of(PostSummaryResponse.builder().postId(10L).imageUrls(List.of("https://img/a.jpg")).build()))
                    .page(0)
                    .size(20)
                    .totalElements(1)
                    .totalPages(1)
                    .hasNext(false)
                    .build();
            given(folderRepository.findById(3L)).willReturn(Optional.of(folder));
            given(scrapRepository.findByUserAndFolderAndPostDeletedFalseOrderByCreatedAtDesc(eq(user), eq(folder), any(Pageable.class)))
                    .willReturn(scrapPage);
            given(postQueryService.toSummaryPageResponse(any(), eq(user))).willReturn(summaries);

            PageResponse<PostSummaryResponse> result = scrapService.getScrapsInFolder(user, 3L, 0, 20);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getPostId()).isEqualTo(10L);
            assertThat(result.getContent().get(0).getImageUrls()).containsExactly("https://img/a.jpg");
        }
    }

    @Nested
    @DisplayName("unscrap")
    class Unscrap {

        @Test
        @DisplayName("삭제된 게시글도 스크랩 취소할 수 있다")
        void unscrapsDeletedPost() {
            AppUser user = appUser(1L);
            Post post = post(10L, user);
            ReflectionTestUtils.setField(post, "deleted", true);
            Folder folder = folder(3L, user, "기본 폴더", true);
            Scrap scrap = Scrap.builder().user(user).post(post).folder(folder).build();
            given(postRepository.findById(10L)).willReturn(Optional.of(post));
            given(scrapRepository.findByUserAndPost(user, post)).willReturn(Optional.of(scrap));

            ScrapResponse result = scrapService.unscrap(user, 10L);

            assertThat(result.isScrapped()).isFalse();
            verify(scrapRepository).delete(scrap);
            verify(postRepository).decrementScrapCount(10L);
        }
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

    private Post post(Long id, AppUser user) {
        Post post = Post.builder()
                .user(user)
                .slug("slug-" + id)
                .title("제목")
                .content("본문")
                .category(PostCategory.RECOMMEND)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private Folder folder(Long id, AppUser user, String name, boolean isDefault) {
        Folder folder = Folder.builder()
                .user(user)
                .name(name)
                .isDefault(isDefault)
                .build();
        ReflectionTestUtils.setField(folder, "id", id);
        return folder;
    }
}
